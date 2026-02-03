package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.service.reputation.ReputationCalculationServiceIF;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardAbstractEvent;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.CacheBadgeAwardReputationEventServiceIF;
import com.prosilion.superconductor.base.service.CacheBadgeDefinitionReputationEventServiceIF;
import com.prosilion.superconductor.base.service.CacheFormulaEventServiceIF;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindTypePlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG;

@Slf4j
// our SportsCar extends CarDecorator
public class BadgeAwardReputationEventKindTypeRedisPlugin extends PublishingEventKindTypePlugin {
  private final String afterimageRelayUrl;
  private final Identity aImgIdentity;
  private final CacheServiceIF cacheServiceIF;
  private final ReputationCalculationServiceIF reputationCalculationServiceIF;
  private final CacheBadgeAwardReputationEventServiceIF cacheBadgeAwardReputationEventService;
  private final CacheBadgeDefinitionReputationEventServiceIF cacheBadgeDefinitionReputationEventServiceIF;
  private final CacheFormulaEventServiceIF cacheFormulaEventServiceIF;

  public BadgeAwardReputationEventKindTypeRedisPlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull Identity aImgIdentity,
      @NonNull NotifierService notifierService,
      @NonNull EventKindTypePluginIF eventKindTypePlugin,
      @NonNull @Qualifier("redisCacheService") CacheServiceIF cacheServiceIF,
      @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF,
      @NonNull CacheBadgeAwardReputationEventServiceIF cacheBadgeAwardReputationEventService,
      @NonNull CacheBadgeDefinitionReputationEventServiceIF cacheBadgeDefinitionReputationEventServiceIF,
      @NonNull CacheFormulaEventServiceIF cacheFormulaEventServiceIF) {
    super(notifierService, eventKindTypePlugin);
    this.afterimageRelayUrl = afterimageRelayUrl;
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.reputationCalculationServiceIF = reputationCalculationServiceIF;
    this.cacheBadgeAwardReputationEventService = cacheBadgeAwardReputationEventService;
    this.cacheBadgeDefinitionReputationEventServiceIF = cacheBadgeDefinitionReputationEventServiceIF;
    this.cacheFormulaEventServiceIF = cacheFormulaEventServiceIF;
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF incomingFollowSetsEventAsReputationEvent) {
    PublicKey awardRecipientPublicKey = Filterable.getTypeSpecificTags(PubKeyTag.class, incomingFollowSetsEventAsReputationEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

    Relay incomingFollowSetsRelay = Filterable.getTypeSpecificTags(RelayTag.class, incomingFollowSetsEventAsReputationEvent)
        .stream()
        .map(RelayTag::getRelay)
        .findFirst().orElseThrow();

    Optional<BadgeAwardReputationEvent> existingBadgeAwardReputationEvent = cacheBadgeAwardReputationEventService.getEvent(
        awardRecipientPublicKey,
        incomingFollowSetsEventAsReputationEvent.getPublicKey());

    existingBadgeAwardReputationEvent.ifPresent(this::deletePreviousBadgeAwardReputationEvent);

    List<GenericEventRecord> formulaEventsAsGenericEventRecords = cacheServiceIF.getByKind(
        Kind.ARBITRARY_CUSTOM_APP_DATA);

    List<FormulaEvent> formulaEvents = formulaEventsAsGenericEventRecords.stream()
        .map(genericEventRecord -> cacheFormulaEventServiceIF.getEvent(
            genericEventRecord.getId(),
            Filterable.getTypeSpecificTagsStream(RelayTag.class, genericEventRecord).findFirst().map(RelayTag::getRelay).map(Relay::getUrl).orElseThrow()))
        .flatMap(Optional::stream).toList();

    List<BadgeDefinitionReputationEvent> existingReputationDefinitionEvents =
        cacheBadgeDefinitionReputationEventServiceIF.getExistingReputationDefinitionEvents().stream()
            .filter(badgeDefinitionReputationEvent ->
                badgeDefinitionReputationEvent.getFormulaEvents().stream()
                    .map(FormulaEvent::asAddressTag)
                    .anyMatch(
                        formulaEvents.stream()
//                            .map(BaseEvent::getGenericEventRecord)
//                            .map(cacheFormulaEventServiceIF::materialize)
                            .map(FormulaEvent::asAddressTag).toList()::contains)).toList();

    List<BadgeAwardReputationEvent> updatedBadgeAwardReputationEvents = existingReputationDefinitionEvents.stream()
        .map(existingReputationDefinitionEvent ->
            createBadgeAwardReputationEvent(
                awardRecipientPublicKey,
                existingReputationDefinitionEvent,
                existingBadgeAwardReputationEvent.map(BadgeAwardAbstractEvent::getContent).map(BigDecimal::new).orElse(BigDecimal.ZERO))).toList();

    List<EventIF> newReputationEvents =
        existingReputationDefinitionEvents.stream()
            .map(existingReputationDefinitionEvent ->
                updatedBadgeAwardReputationEvents.stream().map(updatedBadgeAwardReputationEvent ->
                    reputationCalculationServiceIF.calculateReputationEvent(
                        awardRecipientPublicKey,
                        updatedBadgeAwardReputationEvent,
                        existingReputationDefinitionEvent.getFormulaEvents(),
                        (FollowSetsEvent) incomingFollowSetsEventAsReputationEvent)).toList()).flatMap(Collection::stream).toList();

    newReputationEvents.forEach(newReputationEvent ->
        super.processIncomingEvent(newReputationEvent));
  }

  private BadgeAwardReputationEvent createBadgeAwardReputationEvent(
      PublicKey badgeReceiverPubkey,
      BadgeDefinitionReputationEvent badgeDefinitionReputationEvent,
      BigDecimal score) {
    BadgeAwardReputationEvent badgeAwardReputationEvent = new BadgeAwardReputationEvent(
        aImgIdentity,
        badgeReceiverPubkey,
        new Relay(afterimageRelayUrl),
        BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG,
        badgeDefinitionReputationEvent,
        score);

    return badgeAwardReputationEvent;
  }

  private void deletePreviousBadgeAwardReputationEvent(EventIF previousReputationEvent) {
    cacheServiceIF.deleteEvent(
        new DeletionEvent(
            aImgIdentity,
            List.of(new EventTag(previousReputationEvent.getId())), "aImg delete previous REPUTATION event"));
  }
}
