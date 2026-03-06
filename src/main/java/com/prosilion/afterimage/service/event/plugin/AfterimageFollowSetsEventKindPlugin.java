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
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.cache.CacheBadgeAwardGenericEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.PublishingEventKindPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.request.subscriber.NotifierService;
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
  private final CacheBadgeAwardGenericEventServiceIF<BadgeDefinitionGenericEvent, BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> cacheBadgeAwardGenericEventServiceIF;
  private final EventKindTypePluginIF reputationEventPlugin;
  private final Relay relay;

  public AfterimageFollowSetsEventKindPlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull NotifierService notifierService,
      @NonNull EventPlugin eventPlugin,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
      @NonNull CacheBadgeAwardGenericEventServiceIF<BadgeDefinitionGenericEvent, BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> cacheBadgeAwardGenericEventServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull EventKindTypePluginIF reputationEventPlugin) {
    super(notifierService, eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.reputationEventPlugin = reputationEventPlugin;
    this.cacheFollowSetsEventServiceIF = cacheFollowSetsEventServiceIF;
    this.cacheBadgeAwardGenericEventServiceIF = cacheBadgeAwardGenericEventServiceIF;
    this.relay = new Relay(afterimageRelayUrl);
  }

  @Override
  public GenericEventRecord processIncomingEvent(@NonNull EventIF incomingFollowSetsEvent) {
    log.debug("AfterimageFollowSetsEventPlugin processing incoming Kind[{}]:{}\n{}",
        incomingFollowSetsEvent.getKind().getValue(),
        incomingFollowSetsEvent.getKind().getName().toUpperCase(),
        incomingFollowSetsEvent.createPrettyPrintJson());

    FollowSetsEvent materializediIcomingFollowSetsEvent = (FollowSetsEvent) incomingFollowSetsEvent;

//    FollowSetsEvent materializediIcomingFollowSetsEvent = cacheFollowSetsEventServiceIF.materialize(incomingFollowSetsEvent.asGenericEventRecord());

    PublicKey publicKey = materializediIcomingFollowSetsEvent.getBadgeAwardGenericEvents().stream().map(b -> b.getTypeSpecificTags(PubKeyTag.class).getFirst()).map(PubKeyTag::getPublicKey).findFirst().orElseThrow();

    Optional<GenericEventRecord> eventsByKindAndPubKeyTagAndIdentifierTag = cacheServiceIF.getEventsByKindAndPubKeyTagAndIdentifierTag(
        Kind.FOLLOW_SETS,
        publicKey,
        materializediIcomingFollowSetsEvent.getIdentifierTag()).stream().findFirst();

    Optional<FollowSetsEvent> existingFollowSetsEvent = eventsByKindAndPubKeyTagAndIdentifierTag.stream()
        .map(genericEventRecord ->
            getEvent(genericEventRecord, materializediIcomingFollowSetsEvent))
        .flatMap(Optional::stream)
        .findFirst();

//    TODO: ifPresent likely superfluous if delete mechanism already handles optional
    existingFollowSetsEvent.ifPresent(this::deletePreviousFollowSetsEvent);

    List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> existingBadgeAwardGenericVoteEvents =
        existingFollowSetsEvent.map(
                FollowSetsEvent::getContainedAddressableEvents)
            .orElse(List.of())
            .stream()
            .map(eventTag -> cacheBadgeAwardGenericEventServiceIF.getEvent(eventTag.idEvent(), eventTag.getRecommendedRelayUrl()))
            .flatMap(Optional::stream).toList();

    List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> nonMatchingBadgeAwardGenericVoteEvents =
        materializediIcomingFollowSetsEvent.getBadgeAwardGenericEvents().stream()
            .filter(incomingBadgeAwardVoteEvent ->
                !existingBadgeAwardGenericVoteEvents.contains(incomingBadgeAwardVoteEvent)).toList();

    PublicKey voteReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, materializediIcomingFollowSetsEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

    IdentifierTag identifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, materializediIcomingFollowSetsEvent)
        .stream()
        .findFirst().orElseThrow();

    FollowSetsEvent notifierFollowSetsEvent = createFollowSetsEvent(
        voteReceiverPubkey,
        identifierTag,
        Stream.concat(
            existingBadgeAwardGenericVoteEvents.stream(),
            nonMatchingBadgeAwardGenericVoteEvents.stream()).toList());
    
//    TODO: below explicit save circumvents current issue w/ eventPlugin failing (due to Event remote fetch issue),  needs revisit
    cacheServiceIF.save(notifierFollowSetsEvent);
//    TODO: since above is saved, below should now find event locally- then publish it
    super.processIncomingEvent(notifierFollowSetsEvent);

    FollowSetsEvent followsSetAsReputationEvent = createFollowSetsEvent(
        voteReceiverPubkey,
        identifierTag,
        nonMatchingBadgeAwardGenericVoteEvents);
    return reputationEventPlugin.processIncomingEvent(followsSetAsReputationEvent);
  }

  private Optional<FollowSetsEvent> getEvent(GenericEventRecord genericEventRecord, FollowSetsEvent materializediIcomingFollowSetsEvent) {
    Optional<FollowSetsEvent> event = cacheFollowSetsEventServiceIF.getEvent(
        genericEventRecord.getId(),
        materializediIcomingFollowSetsEvent.getRelayTagRelay().getUrl());
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
