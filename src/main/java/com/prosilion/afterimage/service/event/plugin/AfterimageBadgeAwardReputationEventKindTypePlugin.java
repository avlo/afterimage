package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.service.reputation.ReputationCalculationServiceIF;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BadgeSetsEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.base.cache.CacheBadgeAwardReputationEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.BadgeAwardReputationEventKindTypePlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.request.subscriber.NotifierService;
import com.prosilion.superconductor.lib.redis.service.RedisCacheService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
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
  private final CacheBadgeAwardReputationEventServiceIF cacheBadgeAwardReputationEventServiceIF;

  public AfterimageBadgeAwardReputationEventKindTypePlugin(
     @NonNull String afterimageRelayUrl,
     @NonNull Identity aImgIdentity,
     @NonNull NotifierService notifierService,
     @NonNull EventKindTypePluginIF eventKindTypePlugin,
     @NonNull RedisCacheService redisCacheService,
     @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull CacheBadgeAwardReputationEventServiceIF cacheBadgeAwardReputationEventServiceIF) {
    super(notifierService, eventKindTypePlugin);
    this.afterimageRelayUrl = afterimageRelayUrl;
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = redisCacheService;
    this.reputationCalculationServiceIF = reputationCalculationServiceIF;
    this.cacheFollowSetsEventServiceIF = cacheFollowSetsEventService;
    this.cacheBadgeAwardReputationEventServiceIF = cacheBadgeAwardReputationEventServiceIF;
    log.debug("using afterimageRelayUrl: [{}]", afterimageRelayUrl);
  }

  @Override
  public Optional<GenericEventRecord> processIncomingEvent(@NonNull EventIF incomingFollowSetsEventAsReputationEvent, @NonNull Relay relay) {
    log.debug("processing incoming Kind[{}]:{}\n{}",
       incomingFollowSetsEventAsReputationEvent.getKind().getValue(),
       incomingFollowSetsEventAsReputationEvent.getKind().getName().toUpperCase(),
       incomingFollowSetsEventAsReputationEvent.createPrettyPrintJson());

    FollowSetsEvent materializedIncomingFollowSetsEvent = cacheFollowSetsEventServiceIF.materialize(incomingFollowSetsEventAsReputationEvent).orElseThrow();
    log.debug("(0ofY) ... materializedIncomingFollowSetsEvent:\n{}", materializedIncomingFollowSetsEvent.createPrettyPrintJson());

    List<BadgeDefinitionReputationEvent> existingReputationDefinitionEvents =
       materializedIncomingFollowSetsEvent.getBadgeSetsEventList().stream()
          .map(BadgeSetsEvent::getBadgeDefinitionReputationEvent).toList();

    List<BadgeAwardReputationEvent> existingBadgeAwardReputationEvents =
       Optional.of(cacheFollowSetsEventServiceIF.getBadgeAwardReputationEvents(materializedIncomingFollowSetsEvent))
          .filter(Predicate.not(List::isEmpty))
          .orElseGet(() ->
             existingReputationDefinitionEvents.stream().map(existingReputationDefinitionEvent ->
                createBadgeAwardReputationEvent(
                   materializedIncomingFollowSetsEvent.getAwardRecipientPublicKey(),
                   existingReputationDefinitionEvent,
                   new BigDecimal("0"))).toList());

    List<BadgeAwardReputationEvent> updatedBadgeAwardReputationEvents = existingReputationDefinitionEvents.stream()
       .map(badgeDefinitionReputationEvent ->
          existingBadgeAwardReputationEvents.stream()
             .map(existingBadgeAwardReputationEvent ->
                createBadgeAwardReputationEvent(
                   materializedIncomingFollowSetsEvent.getAwardRecipientPublicKey(),
                   badgeDefinitionReputationEvent,
                   new BigDecimal(
                      existingBadgeAwardReputationEvent.getContent().isBlank() ?
                         "0" : existingBadgeAwardReputationEvent.getContent()))))
       .flatMap(Stream::distinct).toList();

    List<BadgeAwardReputationEvent> newReputationEvents = updatedBadgeAwardReputationEvents.stream()
       .map(updatedBadgeAwardReputationEvent ->
          existingReputationDefinitionEvents.stream()
             .map(existingReputationDefinitionEvent ->
                reputationCalculationServiceIF.calculateReputationEvent(
                   materializedIncomingFollowSetsEvent.getAwardRecipientPublicKey(),
                   updatedBadgeAwardReputationEvent,
                   existingReputationDefinitionEvent.getFormulaEvents(),
                   (FollowSetsEvent) incomingFollowSetsEventAsReputationEvent)))
       .flatMap(Stream::distinct).toList();

    log.debug("(6ofY) ... newReputationEvent:\n  {}", newReputationEvents.stream().map(EventIF::createPrettyPrintJson));

//    TODO: possibly reverse order below
    existingBadgeAwardReputationEvents.forEach(this::deletePreviousBadgeAwardReputationEvent); // delete old

    List<GenericEventRecord> genericEventRecords = newReputationEvents.stream()
       .map(newReputationEvent ->
          super.processIncomingEvent(newReputationEvent, relay)).flatMap(Optional::stream).toList();

    return genericEventRecords.stream().findFirst();
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
