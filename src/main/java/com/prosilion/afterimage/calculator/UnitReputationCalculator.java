package com.prosilion.afterimage.calculator;

import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.config.ScoreVoteEvents;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.afterimage.service.reputation.CalculatorLocalService;
import com.prosilion.afterimage.service.reputation.CalculatorServiceIF;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import com.prosilion.superconductor.lib.redis.dto.GenericNosqlEntityKindTypeDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;

public class UnitReputationCalculator implements ReputationCalculatorIF {
  private final CalculatorServiceIF calculatorServiceIF;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;
  private final Identity aImgIdentity;

  public UnitReputationCalculator(
      @NonNull CalculatorServiceIF calculatorServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.calculatorServiceIF = calculatorServiceIF;
    this.aImgIdentity = aImgIdentity;
  }

  public EventIF calculateReputationEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull Optional<EventIF> previousReputationEvent,
      @NonNull EventIF incomingFollowSetsEvent) throws NostrException {
    return createReputationEvent(
        voteReceiverPubkey,
        calculatorServiceIF.calculate(
            new ScoreVoteEvents(
                previousReputationEvent
                    .map(EventIF::getContent)
                    .map(BigDecimal::new)
                    .orElse(BigDecimal.ZERO),
                incomingFollowSetsEvent.getTags().stream()
                    .filter(AddressTag.class::isInstance)
                    .map(AddressTag.class::cast)
                    .map(AddressTag::getIdentifierTag)
                    .map(identifierTag ->
                        Optional.ofNullable(identifierTag).orElseThrow(() -> new InvalidTagException(
                            "NULL", CalculatorLocalService.VALID_TYPES)))
                    .map(IdentifierTag::getUuid)
                    .toList())));
  }

  private GenericEventKindTypeIF createReputationEvent(@NonNull PublicKey badgeReceiverPubkey, @NonNull BigDecimal score) throws NostrException {
    return new GenericNosqlEntityKindTypeDto(
        new BadgeAwardReputationEvent(
            aImgIdentity,
            badgeReceiverPubkey,
            reputationBadgeDefinitionEvent,
            List.of(
                new IdentifierTag(
                    getFullyQualifiedCalculatorName())),
            score),
        AfterimageKindType.UNIT_REPUTATION).convertBaseEventToGenericEventKindTypeIF();
  }

  @Override
  public String getFullyQualifiedCalculatorName() {
    return getClass().getName();
  }
}
