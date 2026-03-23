package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.calculator.DynamicReputationCalculator;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.autoconfigure.base.service.event.award.CacheBadgeAwardGenericEventService;
import com.prosilion.superconductor.base.cache.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.PublishingEventKindPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.request.subscriber.NotifierService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageFollowSetsEventKindPlugin extends PublishingEventKindPlugin { // kind 30_000
  private final Identity aImgIdentity;
  private final CacheServiceIF cacheServiceIF;
  private final CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF;
  private final CacheBadgeAwardGenericEventService cacheBadgeAwardGenericEventService;
  private final EventKindTypePluginIF reputationEventPlugin;
  private final Relay relay;

  public AfterimageFollowSetsEventKindPlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull NotifierService notifierService,
      @NonNull EventPlugin eventPlugin,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
      @NonNull CacheBadgeAwardGenericEventService cacheBadgeAwardGenericEventService,
      @NonNull Identity aImgIdentity,
      @NonNull EventKindTypePluginIF badgeAwardReputationEventKindTypePlugin) {
    super(notifierService, eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.cacheFollowSetsEventServiceIF = cacheFollowSetsEventServiceIF;
    this.cacheBadgeAwardGenericEventService = cacheBadgeAwardGenericEventService;
    this.reputationEventPlugin = badgeAwardReputationEventKindTypePlugin;
    this.relay = new Relay(afterimageRelayUrl);
  }

  @Override
  public GenericEventRecord processIncomingEvent(@NonNull EventIF incomingFollowSetsEvent) {
    log.debug("processing incoming Kind[{}]: {}\nincomingFollowSetsEvent:\n  {}",
        incomingFollowSetsEvent.getKind().getValue(),
        incomingFollowSetsEvent.getKind().getName().toUpperCase(),
        incomingFollowSetsEvent.createPrettyPrintJson());

    Relay relay = Filterable.getTypeSpecificTagsStream(RelayTag.class, incomingFollowSetsEvent).findFirst().orElseThrow().getRelay();

    log.debug("... cacheFollowSetsEventServiceIF.getEvent();\n  {}\nfrom relay...:\n [{}]",
        incomingFollowSetsEvent.createPrettyPrintJson(), relay.getUrl());

    PublicKey voteReceiverPublicKey = Filterable.getTypeSpecificTags(PubKeyTag.class, incomingFollowSetsEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

    IdentifierTag identifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, incomingFollowSetsEvent)
        .stream()
        .findFirst().orElseThrow();

    log.debug("... using vote recipient public key...:\n  {}", voteReceiverPublicKey.toHexString());

    Optional<GenericEventRecord> eventsByKindAndPubKeyTagAndIdentifierTag = cacheServiceIF.getEventsByKindAndPubKeyTagAndIdentifierTag(
        Kind.FOLLOW_SETS,
        voteReceiverPublicKey,
        identifierTag).stream().findFirst();

    Optional<FollowSetsEvent> existingFollowSetsEvent =
        eventsByKindAndPubKeyTagAndIdentifierTag.stream()
            .map(genericEventRecord ->
                cacheFollowSetsEventServiceIF.getEvent(
                    genericEventRecord.getId(),
                    relay.getUrl()))
            .flatMap(Optional::stream)
            .findFirst();

//    TODO: ifPresent likely superfluous if delete mechanism already handles optional
    existingFollowSetsEvent.ifPresent(this::deletePreviousFollowSetsEvent);

    List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> existingBadgeAwardGenericVoteEvents =
        existingFollowSetsEvent.map(
                FollowSetsEvent::getContainedAddressableEvents)
            .orElse(List.of())
            .stream()
            .map(eventTag -> cacheBadgeAwardGenericEventService.getEvent(eventTag.idEvent(), relay.getUrl()))
            .flatMap(Optional::stream).toList();

    List<EventTag> incomingFollowSetsEventEventTags = Filterable.getTypeSpecificTags(EventTag.class, incomingFollowSetsEvent);

    List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> incomingFollowSetsEventMaterializedEventTags = incomingFollowSetsEventEventTags.stream()
        .map(eventTag -> cacheFollowSetsEventServiceIF.getEventTagEvent(eventTag.getIdEvent(), eventTag.getRecommendedRelayUrl())
            .stream().toList()).flatMap(Collection::stream).toList();

    List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> nonMatchingBadgeAwardGenericVoteEvents =
        incomingFollowSetsEventMaterializedEventTags.stream()
            .filter(incomingBadgeAwardVoteEvent ->
                !existingBadgeAwardGenericVoteEvents.contains(incomingBadgeAwardVoteEvent)).toList();

    FollowSetsEvent notifierFollowSetsEvent = createFollowSetsEvent(
        voteReceiverPublicKey,
        identifierTag,
        Stream.concat(
            existingBadgeAwardGenericVoteEvents.stream(),
            nonMatchingBadgeAwardGenericVoteEvents.stream()).toList());

//    TODO: below explicit save circumvents current issue w/ eventPlugin failing (due to Event remote fetch issue),  needs revisit
    cacheServiceIF.save(notifierFollowSetsEvent);
//    TODO: since above is saved, below should now find event locally- then publish it
    super.processIncomingEvent(notifierFollowSetsEvent);

    FollowSetsEvent followsSetAsReputationEvent = createFollowSetsEvent(
        voteReceiverPublicKey,
        identifierTag,
        nonMatchingBadgeAwardGenericVoteEvents);
    return reputationEventPlugin.processIncomingEvent(followsSetAsReputationEvent);
  }

  private Optional<FollowSetsEvent> getEvent(GenericEventRecord genericEventRecord, String url) {
    Optional<FollowSetsEvent> event = cacheFollowSetsEventServiceIF.getEvent(
        genericEventRecord.getId(),
        url);
    return event;
  }

  private FollowSetsEvent createFollowSetsEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull IdentifierTag identifierTag,
      @NonNull List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> badgeAwardGenericVoteEvents) {
    return new FollowSetsEvent(
        aImgIdentity,
        voteReceiverPubkey,
        identifierTag,
        relay,
        badgeAwardGenericVoteEvents,
        DynamicReputationCalculator.class.getSimpleName());
  }

  private void deletePreviousFollowSetsEvent(FollowSetsEvent previousFollowSetsEvent) {
    cacheServiceIF.deleteEvent(
        new DeletionEvent(
            aImgIdentity,
            List.of(new EventTag(
                previousFollowSetsEvent.getId(),
                previousFollowSetsEvent.getRelayTagRelay().getUrl())), "aImg delete previous FOLLOW_SETS event"));
  }

  @Override
  public Kind getKind() {
    log.debug("getKind Kind[{}]: {}",
        Kind.FOLLOW_SETS.getValue(),
        Kind.FOLLOW_SETS.getName().toUpperCase());
    return Kind.FOLLOW_SETS;
  }
}
