package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.service.reputation.ReputationCalculationServiceIF;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardAbstractEvent;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFormulaEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.award.CacheBadgeAwardReputationEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.base.cache.CacheBadgeAwardReputationEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheBadgeDefinitionGenericEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheBadgeDefinitionReputationEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheFormulaEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.BadgeAwardReputationEventKindTypePlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.request.subscriber.NotifierService;
import com.prosilion.superconductor.lib.redis.service.RedisCacheService;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG;

@Slf4j
// our SportsCar extends CarDecorator
public class AfterimageBadgeAwardReputationEventKindTypePlugin extends BadgeAwardReputationEventKindTypePlugin {
  private final String afterimageRelayUrl;
  private final Identity aImgIdentity;
  private final CacheServiceIF cacheServiceIF;
  private final CacheBadgeDefinitionGenericEventServiceIF cacheBadgeDefinitionGenericEventServiceIF;
  private final ReputationCalculationServiceIF reputationCalculationServiceIF;
  private final CacheBadgeAwardReputationEventServiceIF cacheBadgeAwardReputationEventService;
  private final CacheBadgeDefinitionReputationEventServiceIF cacheBadgeDefinitionReputationEventServiceIF;
  private final CacheFormulaEventServiceIF cacheFormulaEventServiceIF;
  private final CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF;

  public AfterimageBadgeAwardReputationEventKindTypePlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull Identity aImgIdentity,
      @NonNull NotifierService notifierService,
      @NonNull EventKindTypePluginIF eventKindTypePlugin,
      @NonNull RedisCacheService redisCacheService,
      @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF,
      @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
      @NonNull CacheBadgeAwardReputationEventService cacheBadgeAwardReputationEventService,
      @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
      @NonNull CacheFormulaEventService cacheFormulaEventService,
      @NonNull CacheFollowSetsEventService cacheFollowSetsEventService) {
    super(notifierService, eventKindTypePlugin);
    this.afterimageRelayUrl = afterimageRelayUrl;
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = redisCacheService;
    this.reputationCalculationServiceIF = reputationCalculationServiceIF;
    this.cacheBadgeDefinitionGenericEventServiceIF = cacheBadgeDefinitionGenericEventService;
    this.cacheBadgeAwardReputationEventService = cacheBadgeAwardReputationEventService;
    this.cacheBadgeDefinitionReputationEventServiceIF = cacheBadgeDefinitionReputationEventService;
    this.cacheFormulaEventServiceIF = cacheFormulaEventService;
    this.cacheFollowSetsEventServiceIF = cacheFollowSetsEventService;
  }

  @Override
  public GenericEventRecord processIncomingEvent(@NonNull EventIF incomingFollowSetsEventAsReputationEvent) {
    log.debug("processing incoming Kind[{}]:{}\n{}",
        incomingFollowSetsEventAsReputationEvent.getKind().getValue(),
        incomingFollowSetsEventAsReputationEvent.getKind().getName().toUpperCase(),
        incomingFollowSetsEventAsReputationEvent.createPrettyPrintJson());

    PublicKey awardRecipientPublicKey = Filterable.getTypeSpecificTags(PubKeyTag.class, incomingFollowSetsEventAsReputationEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

//    FollowSetsEvent materializediIcomingFollowSetsEvent = cacheFollowSetsEventServiceIF.materialize(incomingFollowSetsEventAsReputationEvent);
    FollowSetsEvent materializediIcomingFollowSetsEvent = (FollowSetsEvent) incomingFollowSetsEventAsReputationEvent;

    List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> badgeAwardGenericEvents =
        materializediIcomingFollowSetsEvent.getContainedAddressableEvents().stream().map(eventTag ->
                cacheFollowSetsEventServiceIF.getEventTagEvent(eventTag.getIdEvent(), eventTag.getRecommendedRelayUrl()))
            .flatMap(Optional::stream).toList();

//    EventTag followSetsFirstEventTagToVotePointer =
//        materializediIcomingFollowSetsEvent
//            .getContainedAddressableEvents().getFirst();

//    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> reconstructedVote =
//        getReconstructedVote(followSetsFirstEventTagToVotePointer);
//
//    BadgeDefinitionGenericEvent existingBadgeDefinitionGenericEvent =
//        getExistingBadgeDefinitionGenericEvent(reconstructedVote);

    Optional<GenericEventRecord> existingBadgeAwardReputationEventGER =
        cacheServiceIF.getEventsByKindAndPubKeyTagAndAddressTag(
                Kind.BADGE_AWARD_EVENT,
                awardRecipientPublicKey,
                new AddressTag(
                    Kind.BADGE_DEFINITION_EVENT,
                    awardRecipientPublicKey,
                    ((FollowSetsEvent) incomingFollowSetsEventAsReputationEvent).getIdentifierTag()))
            .stream()
            .filter(genericEventRecord ->
                genericEventRecord.getTags().stream().anyMatch(baseTag ->
                    baseTag.getClass().equals(ExternalIdentityTag.class)))
            .findFirst();

    Optional<BadgeAwardReputationEvent> existingBadgeAwardReputationEvent = existingBadgeAwardReputationEventGER.stream()
        .map(genericEventRecord -> cacheBadgeAwardReputationEventService.getEvent(
            genericEventRecord.getId(),
            Filterable.getTypeSpecificTagsStream(RelayTag.class, genericEventRecord).findFirst().map(RelayTag::getRelay).map(Relay::getUrl).orElseThrow()))
        .flatMap(Optional::stream).findFirst();

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
                            .map(FormulaEvent::asAddressTag).toList()::contains)).toList();

    List<BadgeAwardReputationEvent> updatedBadgeAwardReputationEvents = existingReputationDefinitionEvents.stream()
        .map(existingReputationDefinitionEvent ->
            createBadgeAwardReputationEvent(
                awardRecipientPublicKey,
                existingReputationDefinitionEvent,
                existingBadgeAwardReputationEvent.map(BadgeAwardAbstractEvent::getContent).map(BigDecimal::new).orElse(BigDecimal.ZERO))).toList();

    List<BadgeAwardReputationEvent> newReputationEvents
        = existingReputationDefinitionEvents.stream()
        .map(existingReputationDefinitionEvent ->
            updatedBadgeAwardReputationEvents.stream().map(updatedBadgeAwardReputationEvent ->
                reputationCalculationServiceIF.calculateReputationEvent(
                    awardRecipientPublicKey,
                    updatedBadgeAwardReputationEvent,
                    existingReputationDefinitionEvent.getFormulaEvents(),
                    (FollowSetsEvent) incomingFollowSetsEventAsReputationEvent)).toList()).flatMap(Collection::stream).toList();

    return newReputationEvents.stream().map(super::processIncomingEvent).toList().getFirst();
  }

  private BadgeDefinitionGenericEvent getExistingBadgeDefinitionGenericEvent(BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> reconstructedVote) {
    return cacheBadgeDefinitionGenericEventServiceIF
        .getAddressTagEvent(reconstructedVote.getAddressTag()).orElseThrow();
  }


  private BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> getReconstructedVote(EventTag followSetsFirstEventTagToVotePointer) {
    return cacheFollowSetsEventServiceIF
        .getEventTagEvent(
            followSetsFirstEventTagToVotePointer.getIdEvent(),
            followSetsFirstEventTagToVotePointer.getRecommendedRelayUrl()).orElseThrow();
  }

  private BadgeAwardReputationEvent createBadgeAwardReputationEvent(
      PublicKey badgeReceiverPubkey,
      BadgeDefinitionReputationEvent badgeDefinitionReputationEvent,
      BigDecimal score) {
    return new BadgeAwardReputationEvent(
        aImgIdentity,
        badgeReceiverPubkey,
        new Relay(afterimageRelayUrl),
        BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG,
        badgeDefinitionReputationEvent,
        score);
  }

  private void deletePreviousBadgeAwardReputationEvent(EventIF previousReputationEvent) {
    cacheServiceIF.deleteEvent(
        new DeletionEvent(
            aImgIdentity,
            List.of(new EventTag(previousReputationEvent.getId(), afterimageRelayUrl)), "aImg delete previous REPUTATION event"));
  }
}
