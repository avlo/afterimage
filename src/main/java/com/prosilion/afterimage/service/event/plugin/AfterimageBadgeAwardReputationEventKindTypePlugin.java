package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.service.reputation.ReputationCalculationServiceIF;
import com.prosilion.nostr.event.BadgeAwardAbstractEvent;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.SetsPairedEvents;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.base.cache.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.BadgeAwardReputationEventKindTypePlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.request.subscriber.NotifierService;
import com.prosilion.superconductor.lib.redis.service.RedisCacheService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG;

@Slf4j
// our SportsCar extends CarDecorator
public class AfterimageBadgeAwardReputationEventKindTypePlugin extends BadgeAwardReputationEventKindTypePlugin {
  private final String afterimageRelayUrl;
  private final Identity aImgIdentity;
  private final CacheServiceIF cacheServiceIF;
  private final ReputationCalculationServiceIF reputationCalculationServiceIF;
  private final CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF;

  public AfterimageBadgeAwardReputationEventKindTypePlugin(
    @NonNull String afterimageRelayUrl,
    @NonNull Identity aImgIdentity,
    @NonNull NotifierService notifierService,
    @NonNull EventKindTypePluginIF eventKindTypePlugin,
    @NonNull RedisCacheService redisCacheService,
    @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF,
    @NonNull CacheFollowSetsEventService cacheFollowSetsEventService) {
    super(notifierService, eventKindTypePlugin);
    this.afterimageRelayUrl = afterimageRelayUrl;
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = redisCacheService;
    this.reputationCalculationServiceIF = reputationCalculationServiceIF;
    this.cacheFollowSetsEventServiceIF = cacheFollowSetsEventService;
    log.debug("using afterimageRelayUrl: [{}]", afterimageRelayUrl);
  }

  @Override
  public Optional<GenericEventRecord> processIncomingEvent(
    @NonNull EventIF incomingFollowSetsEventAsReputationEvent,
    @NonNull Relay relay) {
    log.debug("processing incoming Kind[{}]:{}\n{}",
      incomingFollowSetsEventAsReputationEvent.getKind().getValue(),
      incomingFollowSetsEventAsReputationEvent.getKind().getName().toUpperCase(),
      incomingFollowSetsEventAsReputationEvent.createPrettyPrintJson());

    Optional<FollowSetsEvent> materializedIncomingFollowSetsEvent = cacheFollowSetsEventServiceIF.materialize(incomingFollowSetsEventAsReputationEvent);
    log.debug("(0ofY) ... materializedIncomingFollowSetsEvent:\n{}", materializedIncomingFollowSetsEvent
      .map(EventIF::createPrettyPrintJson).orElse("EMPTY"));

    List<List<SetsPairedEvents>> setsPairedEventListList = materializedIncomingFollowSetsEvent.map(FollowSetsEvent::getSetsPairedEventsList).stream().toList();

//  PREVIOUS IMPL
//  List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> badgeAwardUpvoteEvents = materializedIncomingFollowSetsEvent.map(FollowSetsEvent::getBadgeAwardGenericEvents).stream().flatMap(Collection::stream).toList();
    setsPairedEventListList.stream().map(setsPairedEvents -> null);


    PublicKey voteRecipientPublicKey = null;
//      materializedIncomingFollowSetsEvent.map(FollowSetsEvent::getAwardRecipientPulicKey).orElseThrow();

    List<BadgeDefinitionGenericEvent> badgeDefinitionGenericEvents = null;
//      badgeAwardUpvoteEvents.stream().map(BadgeAwardAbstractEvent::getBadgeDefinitionEvent).toList();

    log.debug("(2ofY) badgeDefinitionGenericEvents:\n[{}]", badgeDefinitionGenericEvents.stream().map(EventIF::createPrettyPrintJson));

    Optional<BadgeAwardReputationEvent> existingBadgeAwardReputationEvent = null;
//      materializedIncomingFollowSetsEvent.flatMap(cacheFollowSetsEventServiceIF::getBadgeAwardReputationEvent);

    Optional<BadgeDefinitionReputationEvent> existingReputationDefinitionEvent = null;
//      materializedIncomingFollowSetsEvent.map(FollowSetsEvent::getBadgeDefinitionReputationEvent);

    log.debug("(4ofY) ... existingReputationDefinitionEvent:\n{}",
      String.format("  [%s]", existingReputationDefinitionEvent.map(EventIF::createPrettyPrintJson).orElse("EMPTY")));

    Optional<BadgeAwardReputationEvent> updatedBadgeAwardReputationEvent = existingReputationDefinitionEvent.map(e ->
      createBadgeAwardReputationEvent(
        voteRecipientPublicKey,
        e,
        existingBadgeAwardReputationEvent.map(BadgeAwardAbstractEvent::getContent).map(BigDecimal::new).orElse(BigDecimal.ZERO)));

    log.debug("(5ofY) ... updatedBadgeAwardReputationEvent [{}]", updatedBadgeAwardReputationEvent.map(
      EventIF::createPrettyPrintJson).orElse("EMPTY"));


    Optional<BadgeAwardReputationEvent> newReputationEvent = reputationCalculationServiceIF.calculateReputationEvent(
      voteRecipientPublicKey,
      updatedBadgeAwardReputationEvent,
      existingReputationDefinitionEvent.map(BadgeDefinitionReputationEvent::getFormulaEvents).orElse(List.of()),
      (FollowSetsEvent) incomingFollowSetsEventAsReputationEvent);

    log.debug("(6ofY) ... newReputationEvent:\n  {}", newReputationEvent.map(EventIF::createPrettyPrintJson).orElse("EMPTY"));

//    TODO: possibly reverse order below
    existingBadgeAwardReputationEvent.ifPresent(this::deletePreviousBadgeAwardReputationEvent); // delete old

    Optional<GenericEventRecord> genericEventRecord = newReputationEvent.flatMap(e ->
      super.processIncomingEvent(e, relay));

    return genericEventRecord;
  }

  private BadgeAwardReputationEvent createBadgeAwardReputationEvent(
    PublicKey badgeReceiverPubkey,
    BadgeDefinitionReputationEvent badgeDefinitionReputationEvent,
    BigDecimal score) {
    return new BadgeAwardReputationEvent(
      aImgIdentity,
      badgeReceiverPubkey,
      BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG,
      badgeDefinitionReputationEvent,
      score,
      new Relay(afterimageRelayUrl));
  }

  private void deletePreviousBadgeAwardReputationEvent(EventIF previousReputationEvent) {
    cacheServiceIF.deleteEvent(
      new DeletionEvent(
        aImgIdentity,
        List.of(new EventTag(previousReputationEvent.getId(), afterimageRelayUrl)), "aImg delete previous REPUTATION event"));
  }
}
