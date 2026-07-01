package com.prosilion.afterimage.calculator;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BadgeSetsEvent;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;

public class DynamicReputationCalculator implements ReputationCalculatorIF {
  private final Identity aImgIdentity;
  private final String afterimageRelayUrl;

  public DynamicReputationCalculator(
    @NonNull String afterimageRelayUrl,
    @NonNull Identity aImgIdentity) {
    this.aImgIdentity = aImgIdentity;
    this.afterimageRelayUrl = afterimageRelayUrl;
  }

  public Optional<BadgeAwardReputationEvent> calculateUpdatedReputationEvent(
    @NonNull PublicKey voteReceiverPubkey,
    @NonNull Optional<BadgeAwardReputationEvent> previousReputationEvent,
    @NonNull List<FormulaEvent> formulaEvents,
    @NonNull FollowSetsEvent incomingFollowSetsEvent) throws NostrException {
    return previousReputationEvent.map(previousEvent ->
      createReputationEvent(
        voteReceiverPubkey,
        calculateReputationEventScore(
          incomingFollowSetsEvent.getBadgeSetsEvents().stream()
            .map(BadgeSetsEvent::getBadgeDefinitionReputationEvent)
            .map(BadgeDefinitionReputationEvent::getFormulaEvents)
            .flatMap(Collection::stream)
            .map(FormulaEvent::getIdentifierTag)
            .filter(formulaEvents.stream()
              .map(FormulaEvent::getBadgeDefinitionGenericEvent)
              .map(BadgeDefinitionGenericEvent::getIdentifierTag)
              .collect(Collectors.toSet())::contains)
            .toList(),
          previousReputationEvent,
          formulaEvents),
        previousEvent.getBadgeDefinitionEvent()));
  }

  private String calculateReputationEventScore(
    List<IdentifierTag> formulaUuids,
    Optional<BadgeAwardReputationEvent> previousReputationEvent,
    List<FormulaEvent> formulaEvents) {
    return formulaUuids.stream()
      .map(formulaUuid -> formulaEvents.stream().collect(
        Collectors.toMap(
          event -> event.getBadgeDefinitionGenericEvent().getIdentifierTag().getUuid(),
          FormulaEvent::getFormula,
          (prev, next) -> next)).get(formulaUuid.getUuid()))
      .filter(Objects::nonNull)
      .reduce(previousReputationEvent
        .map(BadgeAwardReputationEvent::getScore)
        .orElse("0"), ExpressionCalculator::calculate);
  }

  private BadgeAwardReputationEvent createReputationEvent(
    @NonNull PublicKey badgeReceiverPubkey,
    @NonNull String score,
    @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) throws NostrException {
    return new BadgeAwardReputationEvent(
      aImgIdentity,
      badgeReceiverPubkey,
      AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG,
      badgeDefinitionReputationEvent,
      new BigDecimal(score),
      new Relay(afterimageRelayUrl));
  }

  @Override
  public String getFullyQualifiedCalculatorName() {
    return getClass().getName();
  }
}
