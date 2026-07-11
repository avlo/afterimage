package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.AddressableEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BadgeSetsEvent;
import com.prosilion.nostr.event.CurationSetsEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.SetsPairedEvent;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.cache.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.cache.tag.CacheKindAddressTagServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.PublishingEventKindPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.request.subscriber.NotifierService;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AfterimageFollowSetsEventKindPlugin extends PublishingEventKindPlugin { // kind 30_000
  private final Identity aImgIdentity;
  private final CacheServiceIF cacheServiceIF;
  private final CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF;
  private final CacheKindAddressTagServiceIF cacheKindAddressTagServiceIF;
  private final EventKindTypePluginIF badgeAwardReputationEventKindTypePlugin;
  private final Relay relay;

  public AfterimageFollowSetsEventKindPlugin(
     @NonNull String afterimageRelayUrl,
     @NonNull NotifierService notifierService,
     @NonNull EventPlugin eventPlugin,
     @NonNull CacheServiceIF cacheServiceIF,
     @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
     @NonNull CacheKindAddressTagServiceIF cacheKindAddressTagServiceIF,
     @NonNull Identity aImgIdentity,
     @NonNull EventKindTypePluginIF badgeAwardReputationEventKindTypePlugin) {
    super(notifierService, eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.cacheFollowSetsEventServiceIF = cacheFollowSetsEventServiceIF;
    this.cacheKindAddressTagServiceIF = cacheKindAddressTagServiceIF;
    this.badgeAwardReputationEventKindTypePlugin = badgeAwardReputationEventKindTypePlugin;
    this.relay = new Relay(afterimageRelayUrl);
    log.debug("using afterimageRelayUrl: [{}]", afterimageRelayUrl);
  }

  @Override
  public Optional<GenericEventRecord> processIncomingEvent(@NonNull EventIF incomingFollowSetsEvent, @NonNull Relay relay) {
    FollowSetsEvent materializedFollowSetsEvent = cacheFollowSetsEventServiceIF.materialize(incomingFollowSetsEvent).orElseThrow();
    log.debug("materializedFollowSetsEvent:\n{}", materializedFollowSetsEvent.createPrettyPrintJson());

    Set<BadgeDefinitionReputationEvent> existingDefnReputationEvents =
       materializedFollowSetsEvent.getBadgeSetsEventList().stream()
          .map(BadgeSetsEvent::getBadgeDefinitionReputationEvent).collect(Collectors.toSet());

    Set<FollowSetsEvent> awardRecipientExistingFollowSets =
       existingDefnReputationEvents.stream()
          .map(AddressableEvent::asAddressableEventAddressTag)
          .map(aTag -> cacheFollowSetsEventServiceIF.getBy(
             new PubKeyTag(materializedFollowSetsEvent.getAwardRecipientPublicKey()), aTag))
          .flatMap(Collection::stream).collect(Collectors.toSet());
    log.debug("(7of13V) ... awardRecipientExistingFollowSets:\n{}", awardRecipientExistingFollowSets.isEmpty() ? "EMPTY" :
       awardRecipientExistingFollowSets.stream().map(EventIF::createPrettyPrintJson).collect(Collectors.joining(",\n  ")));

    //  filter FollowSetsEvents containing matching badgeDefinitionReputationEvent 
    Set<FollowSetsEvent> targetedFollowSets =
       existingDefnReputationEvents.stream()
          .map(AddressableEvent::asAddressableEventAddressTag)
          .map(addressTagReconstructed ->
             awardRecipientExistingFollowSets.stream()
                .filter(followSetsEvent ->
                   followSetsEvent.getBadgeSetsEventList().stream()
                      .map(BadgeSetsEvent::getCurationSetsEventList).flatMap(Collection::stream)
                      .map(CurationSetsEvent::getAddressTag).toList()
                      .contains(addressTagReconstructed)))
          .flatMap(Stream::distinct)
          .collect(Collectors.toSet());

    List<String> voteEventIds = materializedFollowSetsEvent.getBadgeSetsEventList().stream()
       .map(BadgeSetsEvent::getEventTags).flatMap(Collection::stream).map(EventTag::eventId).toList();

//  if all followSets' badgeSets' award events already contain incoming voteEvent, just return 
    if (!targetedFollowSets.isEmpty() && targetedFollowSets.stream().allMatch(followSetsEvent ->
       followSetsEvent.getBadgeSetsEventList().stream()
          .map(BadgeSetsEvent::getEventTags).flatMap(Collection::stream)
          .map(EventTag::getEventId)
          .anyMatch(voteEventIds::contains))) {
      return Optional.of(materializedFollowSetsEvent.asGenericEventRecord());
    }

    Set<FollowSetsEvent> followSetsEventToSend = Set.of(materializedFollowSetsEvent);

    if (!targetedFollowSets.isEmpty()) {
      Set<FollowSetsEvent> collectedSet = materializedFollowSetsEvent.getBadgeSetsEventList().stream()
         .map(BadgeSetsEvent::getEventTags)
         .flatMap(Collection::stream)
         .flatMap(eventTag ->
            targetedFollowSets.stream().map(followSetsEvent ->
               followSetsEvent.createNewFromExisting(aImgIdentity,
                  followSetsEvent.getBadgeSetsEventList().stream().map(badgeSetsEvent ->
                  {
                    SetsPairedEvent setsPairedEvent = new SetsPairedEvent(
                       badgeSetsEvent.asAddressableEventAddressTag(),
                       relay,
                       eventTag,
                       materializedFollowSetsEvent.getAwardRecipientPublicKey());
                    CurationSetsEvent curationSetsEvent = new CurationSetsEvent(
                       aImgIdentity,
                       badgeSetsEvent.getBadgeDefinitionReputationEvent(),
                       setsPairedEvent,
                       relay);
                    BadgeSetsEvent newFromExisting = badgeSetsEvent.createNewFromExisting(aImgIdentity,
                       curationSetsEvent);
                    return newFromExisting;
                  }).toList()))).collect(Collectors.toSet());
      followSetsEventToSend = collectedSet;
    }

    log.debug("(10of13V) ... deleting previous targetedFollowSets via forEach(this::deletePreviousFollowSetsEvent) ...");
    targetedFollowSets.forEach(this::deletePreviousFollowSetsEvent);

    log.debug("(11of13V) ... saving new/updated targetedFollowSets via targetedFollowSets.forEach(super.processIncomingEvent) ...");
    followSetsEventToSend.forEach(e -> super.processIncomingEvent(e, relay));

    log.debug("(12of13V) ... calling followSetsEventToSend.foreach(badgeAwardReputationEventKindTypePlugin::processIncomingEvent) ...");
    followSetsEventToSend.forEach(e -> badgeAwardReputationEventKindTypePlugin.processIncomingEvent(e, relay));

    log.debug("(13of13V) ... done.  returning materializedFollowSetsEvent.asGenericEventRecord():\n  {}",
       materializedFollowSetsEvent.createPrettyPrintJson());
    return Optional.of(materializedFollowSetsEvent.asGenericEventRecord());

  }

  private void deletePreviousFollowSetsEvent(FollowSetsEvent previousFollowSetsEvent) {
    cacheServiceIF.deleteEvent(
       new DeletionEvent(
          aImgIdentity,
          List.of(new EventTag(
             previousFollowSetsEvent.getId(),
             previousFollowSetsEvent.getRelay().map(Relay::getUrl).orElse(null))), "aImg delete previous FOLLOW_SETS event"));
  }

  @Override
  public Kind getKind() {
    log.debug("getKind Kind[{}]: {}",
       Kind.FOLLOW_SETS.getValue(),
       Kind.FOLLOW_SETS.getName().toUpperCase());
    return Kind.FOLLOW_SETS;
  }
}
