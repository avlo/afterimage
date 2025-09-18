package com.prosilion.afterimage.calculator;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindType;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import com.prosilion.superconductor.lib.redis.dto.GenericDocumentKindTypeDto;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class UnitReputationCalculator implements ReputationCalculatorIF {
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;
  private final Identity aImgIdentity;

  public UnitReputationCalculator(@NonNull Identity aImgIdentity, @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.aImgIdentity = aImgIdentity;
  }

  public GenericEventKindTypeIF calculateUpdatedReputationEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull Optional<GenericEventKindType> previousReputationEvent,
      @NonNull EventIF incomingFollowSetsEvent) throws NoSuchAlgorithmException {
    return createReputationEvent(
        voteReceiverPubkey,
        calculateReputationEvent(
            previousReputationEvent
                .map(GenericEventKindTypeIF::getContent)
                .map(BigDecimal::new)
                .orElse(BigDecimal.ZERO),
            incomingFollowSetsEvent.getTags().stream()
                .filter(AddressTag.class::isInstance)
                .map(AddressTag.class::cast)
                .map(AddressTag::getIdentifierTag)
                .map(identifierTag ->
                    Optional.ofNullable(identifierTag).orElseThrow())
                .map(IdentifierTag::getUuid).toList()));
  }

  private BigDecimal calculateReputationEvent(BigDecimal previousScore, List<String> voteEvents) throws NostrException {
    return previousScore.add(
        calculateReputationLoop(
            voteEvents.stream()
                .map(this::convertContentToScore).toList()));
  }

  private BigDecimal calculateReputationLoop(List<BigDecimal> uuids) {
    return uuids.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private GenericEventKindTypeIF createReputationEvent(@NonNull PublicKey badgeReceiverPubkey, @NonNull BigDecimal score) throws NostrException, NoSuchAlgorithmException {
    return new GenericDocumentKindTypeDto(
        new BadgeAwardReputationEvent(
            aImgIdentity,
            badgeReceiverPubkey,
            reputationBadgeDefinitionEvent,
            List.of(
                new IdentifierTag(
                    getFullyQualifiedCalculatorName())),
            score),
        AfterimageKindType.REPUTATION).convertBaseEventToGenericEventKindTypeIF();
  }

  private BigDecimal convertContentToScore(String event) {
    return event.equals(SuperconductorKindType.UNIT_UPVOTE.getName()) ? new BigDecimal("1") : new BigDecimal("-1");
  }

  @Override
  public String getFullyQualifiedCalculatorName() {
    return getClass().getName();
  }
}
