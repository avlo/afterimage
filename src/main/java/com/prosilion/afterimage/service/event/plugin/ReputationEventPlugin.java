package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.db.AfterimageCacheService;
import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.afterimage.service.reputation.ReputationCalculationServiceIF;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardAbstractEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.KindTypeIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindTypePlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class ReputationEventPlugin extends PublishingEventKindTypePlugin {
  private final ReputationCalculationServiceIF reputationCalculationServiceIF;
  private final AfterimageCacheService afterimageCacheService;
  private final Identity aImgIdentity;

  public ReputationEventPlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindTypePluginIF eventKindTypePlugin,
      @NonNull AfterimageCacheService afterimageCacheService,
      @NonNull Identity aImgIdentity,
      @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF) {
    super(notifierService, eventKindTypePlugin);
    this.afterimageCacheService = afterimageCacheService;
    this.reputationCalculationServiceIF = reputationCalculationServiceIF;
    this.aImgIdentity = aImgIdentity;
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF incomingReputationEvent) throws NostrException {
    PublicKey voteReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, incomingReputationEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

    IdentifierTag identifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, incomingReputationEvent)
        .stream()
        .findFirst().orElseThrow();

    Optional<BadgeAwardReputationEvent> existingBadgeAwardReputationEvent = getExistingBadgeAwardReputationEvent(
        voteReceiverPubkey,
        incomingReputationEvent.getPublicKey(),
        identifierTag);
    existingBadgeAwardReputationEvent.ifPresent(this::deletePreviousBadgeAwardReputationEvent);

    BadgeDefinitionReputationEvent existingReputationDefinitionEvent = getExistingBadgeDefinitionReputationEvent(
        incomingReputationEvent.getPublicKey(),
        identifierTag);

    BadgeAwardReputationEvent updatedBadgeAwardReputationEvent = createBadgeAwardReputationEvent(
        voteReceiverPubkey,
        existingReputationDefinitionEvent,
//        existingBadgeAwardReputationEvent.orElse(BigDecimal.ZERO.toString()).getContent()
        existingBadgeAwardReputationEvent.map(BadgeAwardAbstractEvent::getContent).map(BigDecimal::new).orElse(BigDecimal.ZERO));

    EventIF newReputationEvent = reputationCalculationServiceIF.calculateReputationEvent(
        voteReceiverPubkey,
        updatedBadgeAwardReputationEvent,
        existingReputationDefinitionEvent.getFormulaEvents(),
        incomingReputationEvent);

    super.processIncomingEvent(newReputationEvent);
  }

  private BadgeDefinitionReputationEvent getExistingBadgeDefinitionReputationEvent(
      @NonNull PublicKey eventCreatorPubkey,
      @NonNull IdentifierTag uuid) {
    BadgeDefinitionReputationEvent foundReputationDefinitionEvent =
        afterimageCacheService.getBadgeDefinitionReputationEvent(
            eventCreatorPubkey,
            uuid);
    return foundReputationDefinitionEvent;
  }

  private Optional<BadgeAwardReputationEvent> getExistingBadgeAwardReputationEvent(
      @NonNull PublicKey badgeReceiverPubkey,
      @NonNull PublicKey eventCreatorPubkey,
      @NonNull IdentifierTag uuid) {
    Optional<BadgeAwardReputationEvent> badgeAwardReputationEvent =
        afterimageCacheService.getBadgeAwardReputationEvent(
            badgeReceiverPubkey,
            eventCreatorPubkey,
            uuid);
    return badgeAwardReputationEvent;
  }

  private BadgeAwardReputationEvent createBadgeAwardReputationEvent(
      PublicKey badgeReceiverPubkey,
      BadgeDefinitionReputationEvent badgeDefinitionReputationEvent,
      BigDecimal score) {
    BadgeAwardReputationEvent badgeAwardReputationEvent = new BadgeAwardReputationEvent(
        aImgIdentity,
        badgeReceiverPubkey,
        badgeDefinitionReputationEvent,
        score);
    return badgeAwardReputationEvent;
  }

  private void deletePreviousBadgeAwardReputationEvent(EventIF previousReputationEvent) {
    afterimageCacheService.deleteEvent(
        new DeletionEvent(
            aImgIdentity,
            List.of(new EventTag(previousReputationEvent.getId())), "aImg delete previous REPUTATION event"));
  }

  @Override
  public Kind getKind() {
    log.debug("{} getKind returning {}}", getClass().getSimpleName(), super.getKind());
    return super.getKind();
  }

  @Override
  public KindTypeIF getKindType() {
    log.debug("{} getKindType returning {}", getClass().getSimpleName(), super.getKindType());
    return super.getKindType();
  }
}
