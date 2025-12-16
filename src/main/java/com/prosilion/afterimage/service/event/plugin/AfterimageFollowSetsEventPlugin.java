package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.calculator.DynamicReputationCalculator;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericVoteEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.CacheBadgeAwardGenericVoteEventServiceIF;
import com.prosilion.superconductor.base.service.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindPlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageFollowSetsEventPlugin extends PublishingEventKindPlugin { // kind 30_000
  private final EventKindTypePluginIF reputationEventPlugin;
  private final CacheServiceIF cacheServiceIF;
  private final Identity aImgIdentity;
  private final CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF;
  private final CacheBadgeAwardGenericVoteEventServiceIF cacheBadgeAwardGenericVoteEventServiceIF;
  private final Relay relay;

  public AfterimageFollowSetsEventPlugin(
      String afterimageRelayUrl,
      @NonNull NotifierService notifierService,
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull @Qualifier("redisCacheService") CacheServiceIF cacheServiceIF,
      @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
      @NonNull CacheBadgeAwardGenericVoteEventServiceIF cacheBadgeAwardGenericVoteEventServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull EventKindTypePluginIF reputationEventPlugin) {
    super(notifierService, eventKindPlugin);
    this.relay = new Relay(afterimageRelayUrl);
    this.reputationEventPlugin = reputationEventPlugin;
    this.cacheServiceIF = cacheServiceIF;
    this.cacheBadgeAwardGenericVoteEventServiceIF = cacheBadgeAwardGenericVoteEventServiceIF;
    this.cacheFollowSetsEventServiceIF = cacheFollowSetsEventServiceIF;
    this.aImgIdentity = aImgIdentity;
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF incomingFollowSetsEvent) {
    log.debug("{}} processing incoming Kind.FOLLOW_SETS 30_000 : [{}]", getClass().getSimpleName(), incomingFollowSetsEvent);

    Optional<FollowSetsEvent> existingFollowSetsEvent = cacheFollowSetsEventServiceIF.getEvent(incomingFollowSetsEvent.getId());
//    TODO: ifPresent likely superfluous if delete mechanism already handles optional
    existingFollowSetsEvent.ifPresent(this::deletePreviousFollowSetsEvent);

    List<EventTag> incomingEventTags = getEventTags(incomingFollowSetsEvent.getTags());
    List<EventTag> existingEventTags = existingFollowSetsEvent.map(FollowSetsEvent::getContainedAddressableEvents).orElse(List.of());

    List<EventTag> nonMatches = incomingEventTags.stream()
        .filter(incomingEventTagAddressTagPair ->
            !existingEventTags.contains(incomingEventTagAddressTagPair)).toList();

    PublicKey voteReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, incomingFollowSetsEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();
//
    IdentifierTag identifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, incomingFollowSetsEvent)
        .stream()
        .findFirst().orElseThrow();

    List<BadgeAwardGenericVoteEvent> incomingBadgeAwardGenericVoteEvents = incomingEventTags.stream().map(EventTag::getIdEvent).map(cacheBadgeAwardGenericVoteEventServiceIF::getEvent).flatMap(Optional::stream).toList();

    List<BadgeAwardGenericVoteEvent> existingBadgeAwardGenericVoteEvents = existingEventTags.stream().map(EventTag::getIdEvent).map(cacheBadgeAwardGenericVoteEventServiceIF::getEvent).flatMap(Optional::stream).toList();

    List<BadgeAwardGenericVoteEvent> nonMatchingBadgeAwardGenericVoteEvents = incomingBadgeAwardGenericVoteEvents.stream()
        .filter(incomingEventTagAddressTagPair ->
            !existingBadgeAwardGenericVoteEvents.contains(incomingEventTagAddressTagPair)).toList();

    List<BadgeAwardGenericVoteEvent> saveToDbVotes = Stream.concat(
        incomingBadgeAwardGenericVoteEvents.stream(), nonMatchingBadgeAwardGenericVoteEvents.stream()).toList();

    EventIF saveToDbFollowSetsEvent = existingFollowSetsEvent.map(followSetsEvent ->
            createFollowSetsEventFromCombinedExistingAndIncomingFollowSets(followSetsEvent, saveToDbVotes))
        .orElse(
            createFreshFollowSetsEvent(voteReceiverPubkey, identifierTag, saveToDbVotes));

    super.processIncomingEvent(saveToDbFollowSetsEvent);

    EventIF createdReputationEvent = createFreshFollowSetsEvent(
        voteReceiverPubkey,
        identifierTag,
        nonMatchingBadgeAwardGenericVoteEvents);
    reputationEventPlugin.processIncomingEvent(createdReputationEvent);
  }

//  private Optional<GenericEventKind> getExistingFollowSetsEvent(
//      PublicKey badgeReceiverPubkey,
//      IdentifierTag uuid) {
//    return cacheServiceIF
//        .getEventsByKindAndPubKeyTag(Kind.FOLLOW_SETS, badgeReceiverPubkey)
//        .stream()
//        .filter(eventIf ->
//            Filterable.getTypeSpecificTags(IdentifierTag.class, eventIf)
//                .contains(uuid))
//        .max(Comparator.comparing(EventIF::getCreatedAt))
//        .map(eventIF ->
//            new GenericEventKind(
//                eventIF.getId(),
//                eventIF.getPublicKey(),
//                eventIF.getCreatedAt(),
//                eventIF.getKind(),
//                eventIF.getTags(),
//                eventIF.getContent(),
//                eventIF.getSignature()));
//  }

  private EventIF createFollowSetsEventFromCombinedExistingAndIncomingFollowSets(
      @NonNull FollowSetsEvent followSetsEvent,
      @NonNull List<BadgeAwardGenericVoteEvent> voteEvents) {

    Function<EventTag, BadgeAwardGenericVoteEvent> fxn = eventTag ->
        voteEvents.stream().filter(badgeAwardAbstractEvent ->
            badgeAwardAbstractEvent.getId().equals(eventTag.getIdEvent())).findFirst().orElseThrow();

    FollowSetsEvent newFromExistingAndIncoming = new FollowSetsEvent(followSetsEvent.getGenericEventRecord(), fxn);

    return newFromExistingAndIncoming;
  }

  private EventIF createFreshFollowSetsEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull IdentifierTag identifierTag,
      @NonNull List<BadgeAwardGenericVoteEvent> badgeAwardGenericVoteEvents) {
    FollowSetsEvent followSetsEvent = new FollowSetsEvent(
        aImgIdentity,
        voteReceiverPubkey,
        identifierTag,
        relay,
        badgeAwardGenericVoteEvents,
        DynamicReputationCalculator.class.getSimpleName());

    GenericEventRecord genericEventRecord = followSetsEvent.getGenericEventRecord();

    return genericEventRecord;
  }

  public List<EventTag> getEventTags(List<BaseTag> followSetsEvent) {
    List<EventTag> eventTags = followSetsEvent
        .stream()
        .filter(EventTag.class::isInstance)
        .map(EventTag.class::cast)
        .toList();
    return eventTags;
  }

  private void deletePreviousFollowSetsEvent(FollowSetsEvent previousFollowSetsEvent) {
    cacheServiceIF.deleteEvent(
        new DeletionEvent(
            aImgIdentity,
            List.of(new EventTag(previousFollowSetsEvent.getId())), "aImg delete previous FOLLOW_SETS event"));
  }

  @Override
  public Kind getKind() {
    log.debug("{} getKind of Kind.FOLLOW_SETS 30_000", getClass().getSimpleName());
    return Kind.FOLLOW_SETS; // 30_000
  }
}
