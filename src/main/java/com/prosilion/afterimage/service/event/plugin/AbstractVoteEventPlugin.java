package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.AddressableEvent;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.tag.SetsPairedEvents;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.base.cache.CacheFormulaEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.BadgeSetsEventKindPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.NonPublishingEventKindPlugin;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// our SportsCar extends CarDecorator
public abstract class AbstractVoteEventPlugin extends NonPublishingEventKindPlugin {
  private final CacheServiceIF cacheServiceIF;
  private final CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService;
  private final CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService;
  private final CacheFollowSetsEventService cacheFollowSetsEventService;
  private final AfterimageFollowSetsEventKindPlugin afterimageFollowSetsEventKindPlugin;
  private final CacheFormulaEventServiceIF cacheFormulaEventServiceIF;
  private final BadgeSetsEventKindPlugin badgeSetsEventKindPlugin;
  private final Identity aImgIdentity;
  private final Relay relay;

  public AbstractVoteEventPlugin(
     @NonNull String afterimageRelayUrl,
     @NonNull CacheServiceIF cacheServiceIF,
     @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
     @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull AfterimageFollowSetsEventKindPlugin afterimageFollowSetsEventKindPlugin,
     @NonNull CacheFormulaEventServiceIF cacheFormulaEventServiceIF,
     @NonNull BadgeSetsEventKindPlugin badgeSetsEventKindPlugin,
     @NonNull EventPlugin eventPlugin,
     @NonNull Identity aImgIdentity) {
    super(eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.cacheBadgeDefinitionGenericEventService = cacheBadgeDefinitionGenericEventService;
    this.cacheBadgeDefinitionReputationEventService = cacheBadgeDefinitionReputationEventService;
    this.cacheFollowSetsEventService = cacheFollowSetsEventService;
    this.afterimageFollowSetsEventKindPlugin = afterimageFollowSetsEventKindPlugin;
    this.cacheFormulaEventServiceIF = cacheFormulaEventServiceIF;
    this.badgeSetsEventKindPlugin = badgeSetsEventKindPlugin;
    this.relay = new Relay(afterimageRelayUrl);
    log.debug("using afterimageRelayUrl: [{}]", afterimageRelayUrl);
  }

  @Override
  public Optional<GenericEventRecord> processIncomingEvent(@NonNull EventIF voteEvent, @NonNull Relay relay) {
    log.debug("processing incoming voteEvent\n{}", voteEvent.createPrettyPrintJson());

    AddressTag voteEventAddressTag = voteEvent.asGenericEventRecord().requireFirstTag(AddressTag.class);
    Relay consolidatedRelay = voteEventAddressTag.findRelay().orElse(relay);
    log.debug("processIncomingEvent(voteEvent, Relay):\n  " +
          "voteEventAddressTag Relay: [ {} ]\n  " +
          "eventual consolidated Relay: [ {} ]",
       voteEventAddressTag.findRelay().map(Relay::getUrl).orElse("NULL"),
       consolidatedRelay.getUrl());

    AddressTag addressTagReconstructed = new AddressTag(
       voteEventAddressTag.getKind(),
       voteEventAddressTag.getPublicKey(),
       voteEventAddressTag.requireIdentifierTag(),
       consolidatedRelay);

    Optional<BadgeDefinitionGenericEvent> existingDefnUpvoteEvent = cacheBadgeDefinitionGenericEventService.getBy(addressTagReconstructed);
    log.debug("(1of13V) existingDefnUpvoteEvent:\n  {}", existingDefnUpvoteEvent
       .map(EventIF::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));
    Optional<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> upvoteEventReconstructedOpt =
       existingDefnUpvoteEvent.map(ev ->
          emptyAddressTagRelayTriesSourceRelay(
             voteEvent, addressTagReconstructed, ev));

    log.debug("(2of13V) upvoteEventReconstructed:\n  {}", upvoteEventReconstructedOpt.map(EventIF::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));
    Optional<FormulaEvent> formulaEvent = existingDefnUpvoteEvent.map(AddressableEvent::asAddressableEventAddressTag).flatMap(cacheFormulaEventServiceIF::getBy);

    log.debug("(3of13V) Optional<FormulaEvent> formulaEvent:\n  {}", formulaEvent.map(EventIF::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));
    Optional<AddressTag> formulaEventAddressableEventAddressTag = formulaEvent.map(AddressableEvent::asAddressableEventAddressTag);

    Optional<BadgeDefinitionReputationEvent> existingDefnReputationEvent =
       formulaEventAddressableEventAddressTag.flatMap(cacheBadgeDefinitionReputationEventService::getByDirectTag).stream().findFirst();
    log.debug("(5of13V) existingDefnReputationEvent:\n  {}", existingDefnReputationEvent.map(EventIF::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));

    List<FollowSetsEvent> awardRecipientExistingFollowSets = existingDefnReputationEvent.map(AddressableEvent::asAddressableEventAddressTag)
       .map(aTag -> cacheFollowSetsEventService.getBy(
          voteEvent.requireFirstTag(PubKeyTag.class), aTag)).stream().flatMap(Collection::stream).toList();
    log.debug("(7of13V) ... awardRecipientExistingFollowSets:\n{}", awardRecipientExistingFollowSets.stream()
       .map(EventIF::createPrettyPrintJson).collect(Collectors.joining(",\n  ")));

//  filter FollowSetsEvents containing matching badgeDefinitionReputationEvent 
    List<FollowSetsEvent> targetedFollowSets = awardRecipientExistingFollowSets.stream()
       .filter(followSetsEvent ->
          followSetsEvent.getSetsPairedEventsList().stream()
             .map(SetsPairedEvents::getAddressTag).toList()
             .contains(addressTagReconstructed)).toList();

//  if all followSets' badgeSets' award events already contain incoming voteEvent, just return 
    if (targetedFollowSets.stream().allMatch(setsPairedEvents ->
       setsPairedEvents.getSetsPairedEventsList().stream()
          .map(SetsPairedEvents::getAwardEventId).toList()
          .contains(voteEvent.getId()))) {
      return upvoteEventReconstructedOpt.map(EventIF::asGenericEventRecord);
    }

    Set<FollowSetsEvent> followSetsEventToSend =
       upvoteEventReconstructedOpt.stream()
          .flatMap(upvote -> {
            var relayUrl = upvote.getRelay().map(Relay::getUrl).orElse(relay.getUrl());
            var upvoteTag = new EventTag(upvote.getId(), relayUrl);
            var awardRecipient = upvote.getAwardRecipientPublicKey();

            return targetedFollowSets.stream()
               .map(followSetsEvent -> followSetsEvent.createNewFromExisting(
                  aImgIdentity,
                  followSetsEvent.getBadgeSetsEvents().stream()
                     .map(badgeSetsEvent -> badgeSetsEvent.createNewFromExisting(
                        aImgIdentity,
                        new SetsPairedEvents(
                           badgeSetsEvent.asAddressableEventAddressTag(),
                           upvoteTag,
                           awardRecipient)))
                     .toList()));
          }).collect(Collectors.toSet());

    log.debug("(12of13V) ... calling followSetsEventToSend.stream().map(afterimageFollowSetsEventKindPlugin::processIncomingEvent) ...");
    followSetsEventToSend.forEach(e -> afterimageFollowSetsEventKindPlugin.processIncomingEvent(e, relay));

    log.debug("(13of13V) ... done.  returning upvoteEventReconstructed.asGenericEventRecord():\n  {}",
       upvoteEventReconstructedOpt.map(EventIF::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));
    return upvoteEventReconstructedOpt.map(EventIF::asGenericEventRecord);
  }

  //  TODO: public only for test, consider rxr
  public static BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> emptyAddressTagRelayTriesSourceRelay(
     EventIF event,
     AddressTag addressTag,
     BadgeDefinitionGenericEvent badgeDefinitionUpvoteEvent) {
    return new BadgeAwardGenericEvent<>(
       new GenericEventRecord(
          event.getId(),
          event.getPublicKey(),
          event.getCreatedAt(),
          event.getKind(),
          List.of(new AddressTag(
                addressTag.getKind(),
                addressTag.getPublicKey(),
                addressTag.requireIdentifierTag(),
                addressTag.requireRelay()),
             new RelayTag(addressTag.requireRelay()),
             event.requireFirstTag(PubKeyTag.class)),
          event.getContent(),
          event.getSignature()), aTag -> badgeDefinitionUpvoteEvent);
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }
}
