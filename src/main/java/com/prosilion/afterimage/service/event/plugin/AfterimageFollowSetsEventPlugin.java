package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.calculator.DynamicReputationCalculator;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.CacheBadgeAwardGenericEventServiceIF;
import com.prosilion.superconductor.base.service.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindPlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageFollowSetsEventPlugin extends PublishingEventKindPlugin { // kind 30_000
  private final Identity aImgIdentity;
  private final CacheServiceIF cacheServiceIF;
  private final CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF;
  private final CacheBadgeAwardGenericEventServiceIF cacheBadgeAwardGenericEventServiceIF;
  private final EventKindTypePluginIF reputationEventPlugin;
  private final Relay relay;

  public AfterimageFollowSetsEventPlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull NotifierService notifierService,
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull @Qualifier("redisCacheService") CacheServiceIF cacheServiceIF,
      @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
      @NonNull CacheBadgeAwardGenericEventServiceIF cacheBadgeAwardGenericEventServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull EventKindTypePluginIF reputationEventPlugin) {
    super(notifierService, eventKindPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.reputationEventPlugin = reputationEventPlugin;
    this.cacheFollowSetsEventServiceIF = cacheFollowSetsEventServiceIF;
    this.cacheBadgeAwardGenericEventServiceIF = cacheBadgeAwardGenericEventServiceIF;
    this.relay = new Relay(afterimageRelayUrl);
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF incomingFollowSetsEvent) {
    log.debug("{}} processing incoming Kind.FOLLOW_SETS 30_000 : [{}]", getClass().getSimpleName(), incomingFollowSetsEvent);

    FollowSetsEvent materializediIcomingFollowSetsEvent = cacheFollowSetsEventServiceIF.materialize(incomingFollowSetsEvent.asGenericEventRecord());
    Optional<FollowSetsEvent> existingFollowSetsEvent = cacheFollowSetsEventServiceIF.getEvent(
        materializediIcomingFollowSetsEvent.getId(),
        materializediIcomingFollowSetsEvent.getRelayTagRelay().getUrl());
//    TODO: ifPresent likely superfluous if delete mechanism already handles optional
    existingFollowSetsEvent.ifPresent(this::deletePreviousFollowSetsEvent);

    List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> existingBadgeAwardGenericVoteEvents =
        existingFollowSetsEvent.map(
                FollowSetsEvent::getContainedAddressableEvents)
            .orElse(List.of())
            .stream()
            .map(cacheBadgeAwardGenericEventServiceIF::getEvent)
            .flatMap(Optional::stream).toList();

    List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> nonMatchingBadgeAwardGenericVoteEvents =
        materializediIcomingFollowSetsEvent.getBadgeAwardGenericEvents().stream()
            .filter(incomingBadgeAwardVoteEvent ->
                !existingBadgeAwardGenericVoteEvents.contains(incomingBadgeAwardVoteEvent)).toList();

    PublicKey voteReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, materializediIcomingFollowSetsEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();
//
    IdentifierTag identifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, materializediIcomingFollowSetsEvent)
        .stream()
        .findFirst().orElseThrow();

    FollowSetsEvent notifierFollowSetsEvent = createFollowSetsEvent(
        voteReceiverPubkey,
        identifierTag,
        Stream.concat(
            existingBadgeAwardGenericVoteEvents.stream(),
            nonMatchingBadgeAwardGenericVoteEvents.stream()).toList());
    super.processIncomingEvent(notifierFollowSetsEvent);

    EventIF followsSetAsReputationEvent = createFollowSetsEvent(
        voteReceiverPubkey,
        identifierTag,
        nonMatchingBadgeAwardGenericVoteEvents);
    reputationEventPlugin.processIncomingEvent(followsSetAsReputationEvent);
  }

  private FollowSetsEvent createFollowSetsEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull IdentifierTag identifierTag,
      @NonNull List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> badgeAwardGenericVoteEvents) {
    FollowSetsEvent followSetsEvent = new FollowSetsEvent(
        aImgIdentity,
        voteReceiverPubkey,
        identifierTag,
        relay,
        badgeAwardGenericVoteEvents,
        DynamicReputationCalculator.class.getSimpleName());

    return followSetsEvent;
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
