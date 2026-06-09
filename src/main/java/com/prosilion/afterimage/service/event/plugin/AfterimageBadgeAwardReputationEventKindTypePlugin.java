package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.service.reputation.ReputationCalculationServiceIF;
import com.prosilion.nostr.event.BadgeAwardAbstractEvent;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.EventTag;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

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
  }

  @Override
  public GenericEventRecord processIncomingEvent(@NonNull EventIF incomingFollowSetsEventAsReputationEvent) {
    log.debug("processing incoming Kind[{}]:{}\n{}",
       incomingFollowSetsEventAsReputationEvent.getKind().getValue(),
       incomingFollowSetsEventAsReputationEvent.getKind().getName().toUpperCase(),
       incomingFollowSetsEventAsReputationEvent.createPrettyPrintJson());

    FollowSetsEvent materializedIncomingFollowSetsEvent = cacheFollowSetsEventServiceIF.materialize(incomingFollowSetsEventAsReputationEvent);
    log.debug("(0ofY) ... materializedIncomingFollowSetsEvent:\n{}", materializedIncomingFollowSetsEvent.createPrettyPrintJson());

    List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> badgeAwardUpvoteEvents = materializedIncomingFollowSetsEvent.getBadgeAwardGenericEvents();
    log.debug("(1ofY) ... badgeAwardUpvoteEvents:\n{}", badgeAwardUpvoteEvents.stream().map(EventIF::createPrettyPrintJson));

    PublicKey voteRecipientPublicKey = materializedIncomingFollowSetsEvent.getAwardRecipientPulicKey();

    List<BadgeDefinitionGenericEvent> badgeDefinitionGenericEvents = badgeAwardUpvoteEvents.stream().map(BadgeAwardAbstractEvent::getBadgeDefinitionEvent).toList();

    log.debug("(2ofY) badgeDefinitionGenericEvents:\n[{}]", badgeDefinitionGenericEvents.stream().map(EventIF::createPrettyPrintJson));

    Optional<BadgeAwardReputationEvent> existingBadgeAwardReputationEvent =
       cacheFollowSetsEventServiceIF.getBadgeAwardReputationEvent(materializedIncomingFollowSetsEvent);

    BadgeDefinitionReputationEvent existingReputationDefinitionEvent = materializedIncomingFollowSetsEvent.getBadgeDefinitionReputationEvent();

    log.debug("(4ofY) ... existingReputationDefinitionEvent:\n{}",
       String.format("  [%s]", existingReputationDefinitionEvent.createPrettyPrintJson()));

    BadgeAwardReputationEvent updatedBadgeAwardReputationEvent =
       createBadgeAwardReputationEvent(
          voteRecipientPublicKey,
          existingReputationDefinitionEvent,
          existingBadgeAwardReputationEvent.map(BadgeAwardAbstractEvent::getContent).map(BigDecimal::new).orElse(BigDecimal.ZERO));

    log.debug("(5ofY) ... updatedBadgeAwardReputationEvent [{}]", updatedBadgeAwardReputationEvent.createPrettyPrintJson());

    BadgeAwardReputationEvent newReputationEvent = reputationCalculationServiceIF.calculateReputationEvent(
       voteRecipientPublicKey,
       updatedBadgeAwardReputationEvent,
       existingReputationDefinitionEvent.getFormulaEvents(),
       (FollowSetsEvent) incomingFollowSetsEventAsReputationEvent);

    log.debug("(6ofY) ... newReputationEvent:\n  {}", newReputationEvent.createPrettyPrintJson());

//    TODO: possibly reverse order below
    existingBadgeAwardReputationEvent.ifPresent(this::deletePreviousBadgeAwardReputationEvent); // delete old
    return super.processIncomingEvent(newReputationEvent); // save new
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
