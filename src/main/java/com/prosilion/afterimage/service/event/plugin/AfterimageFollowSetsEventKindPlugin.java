package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.AddressableEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BadgeSetsEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.service.event.curated.CacheCuratedBadgeAwardGenericEventService;
import com.prosilion.superconductor.base.cache.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AfterimageFollowSetsEventKindPlugin extends PublishingEventKindPlugin { // kind 30_000
  private final Identity aImgIdentity;
  private final CacheServiceIF cacheServiceIF;
  private final CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF;
  private final CacheCuratedBadgeAwardGenericEventService cacheCuratedBadgeAwardGenericEventService;
  private final EventKindTypePluginIF badgeAwardReputationEventKindTypePlugin;

  public AfterimageFollowSetsEventKindPlugin(
     @NonNull String afterimageRelayUrl,
     @NonNull NotifierService notifierService,
     @NonNull EventPlugin eventPlugin,
     @NonNull CacheServiceIF cacheServiceIF,
     @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
     @NonNull CacheCuratedBadgeAwardGenericEventService cacheCuratedBadgeAwardGenericEventService,
     @NonNull Identity aImgIdentity,
     @NonNull EventKindTypePluginIF badgeAwardReputationEventKindTypePlugin) {
    super(notifierService, eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.cacheFollowSetsEventServiceIF = cacheFollowSetsEventServiceIF;
    this.cacheCuratedBadgeAwardGenericEventService = cacheCuratedBadgeAwardGenericEventService;
    this.badgeAwardReputationEventKindTypePlugin = badgeAwardReputationEventKindTypePlugin;
    log.debug("using afterimageRelayUrl: [{}]", afterimageRelayUrl);
  }

  @Override
  public Optional<GenericEventRecord> processIncomingEvent(@NonNull EventIF incomingFollowSetsEvent, @NonNull Relay relay) {
    FollowSetsEvent materializedFollowSetsEvent = cacheFollowSetsEventServiceIF.materialize(incomingFollowSetsEvent).orElseThrow();
    log.debug("materializedFollowSetsEvent:\n{}", materializedFollowSetsEvent.createPrettyPrintJson());

    Set<BadgeDefinitionReputationEvent> incomingFollowSetsDefnReputationEvents =
       materializedFollowSetsEvent.getBadgeSetsEventList().stream()
          .map(BadgeSetsEvent::getBadgeDefinitionReputationEvent).collect(Collectors.toSet());

    Set<FollowSetsEvent> existingDbFollowSets =
       findMatchingFollowSets(
          findAwardRecipientExistingFollowSets(
             materializedFollowSetsEvent,
             incomingFollowSetsDefnReputationEvents),
          incomingFollowSetsDefnReputationEvents);

    Set<String> voteEventIds = materializedFollowSetsEvent.getBadgeSetsEventList().stream()
       .map(BadgeSetsEvent::getEventTags)
       .flatMap(Collection::stream).map(EventTag::getEventId).collect(Collectors.toSet());

    if (alreadyContainsIncomingVotes(existingDbFollowSets, voteEventIds)) {
      return Optional.of(materializedFollowSetsEvent.asGenericEventRecord());
    }

    Set<FollowSetsEvent> followSetsEventToSend = existingDbFollowSets.isEmpty()
       ? Set.of(materializedFollowSetsEvent)
       : rebuildFollowSets(materializedFollowSetsEvent, existingDbFollowSets);

    log.debug("(10of13V) ... deleting previous existingDbFollowSets via forEach(this::deletePreviousFollowSetsEvent) ...");
    existingDbFollowSets.forEach(this::deletePreviousFollowSetsEvent);

    log.debug("(11of13V) ... saving new/updated existingDbFollowSets via existingDbFollowSets.forEach(super.processIncomingEvent) ...");
    followSetsEventToSend.forEach(e -> super.processIncomingEvent(e, relay));

    log.debug("(12of13V) ... calling followSetsEventToSend.foreach(badgeAwardReputationEventKindTypePlugin::processIncomingEvent) ...");
    followSetsEventToSend.forEach(e -> badgeAwardReputationEventKindTypePlugin.processIncomingEvent(e, relay));

    log.debug("(13of13V) ... done.  returning materializedFollowSetsEvent.asGenericEventRecord():\n  {}",
       materializedFollowSetsEvent.createPrettyPrintJson());
    return Optional.of(materializedFollowSetsEvent.asGenericEventRecord());

  }

  private Set<FollowSetsEvent> findAwardRecipientExistingFollowSets(FollowSetsEvent followSetsEvent, Set<BadgeDefinitionReputationEvent> defnReputationEvents) {
    return new HashSet<>(cacheFollowSetsEventServiceIF.getBy(new PubKeyTag(followSetsEvent.getAwardRecipientPublicKey())));
  }

  private Set<FollowSetsEvent> findMatchingFollowSets(Set<FollowSetsEvent> awardRecipientFollowSets, Set<BadgeDefinitionReputationEvent> badgeDefinitions) {
    return awardRecipientFollowSets.stream()
       .filter(awardRecipientFollowSetsEvent -> badgeDefinitions.stream()
          .anyMatch(badgeDefinition -> awardRecipientFollowSetsEvent.getBadgeSetsEventList().stream()
             .map(BadgeSetsEvent::getBadgeDefinitionReputationEvent)
             .map(BadgeDefinitionReputationEvent::asAddressableEventAddressTag)
             .anyMatch(badgeDefinition.asAddressableEventAddressTag()::equals)))
       .collect(Collectors.toSet());
  }

  private boolean alreadyContainsIncomingVotes(Set<FollowSetsEvent> followSetsEvents, Set<String> incomingVoteEventIds) {
    return !followSetsEvents.isEmpty() && followSetsEvents.stream()
       .allMatch(followSetsEvent -> followSetsEvent.getBadgeSetsEventList().stream()
          .map(BadgeSetsEvent::getEventTags)
          .flatMap(Collection::stream)
          .map(EventTag::getEventId)
          .anyMatch(incomingVoteEventIds::contains));
  }

  private Set<FollowSetsEvent> rebuildFollowSets(FollowSetsEvent materializedFollowSetsEvent, Set<FollowSetsEvent> existingFollowSetsEvents) {
    return
       materializedFollowSetsEvent.getBadgeSetsEventList().stream()
          .map(BadgeSetsEvent::getEventTags)
          .flatMap(Collection::stream)
          .map(eventTag -> cacheCuratedBadgeAwardGenericEventService.getByDirect(eventTag).orElseThrow())
          .flatMap(badgeAwardEvent -> existingFollowSetsEvents.stream()
             .map(existingFollowSetsEvent -> existingFollowSetsEvent.createNewFromExisting(
                aImgIdentity,
                existingFollowSetsEvent.getBadgeSetsEventList().stream()
                   .map(existingBadgeSetsEvent -> existingBadgeSetsEvent.createNewFromExisting(
                      aImgIdentity, badgeAwardEvent))
                   .toList())))
          .collect(Collectors.toSet());
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
