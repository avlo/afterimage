package com.prosilion.afterimage.calculator;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BadgeSetsEvent;
import com.prosilion.nostr.event.CuratedBadgeAwardGenericEvent;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
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

  public BadgeAwardReputationEvent calculateUpdatedReputationEvent(
     @NonNull PublicKey voteReceiverPubkey,
     @NonNull BadgeAwardReputationEvent previousReputationEvent,
     @NonNull List<FormulaEvent> formulaEvents,
     @NonNull FollowSetsEvent incomingFollowSetsEvent) throws NostrException {
    return
       createReputationEvent(
          voteReceiverPubkey,
          calculateReputationEventScore(
             formulaEvents.stream()
                .filter(formulaEvent ->
                   incomingFollowSetsEvent.getBadgeSetsEventList().stream()
                      .map(BadgeSetsEvent::getCuratedBadgeAwardGenericEventList)
                      .flatMap(Collection::stream)
                      .map(CuratedBadgeAwardGenericEvent::getAddressTag)
                      .toList().contains(
                         formulaEvent.getBadgeDefinitionGenericEvent().asAddressableEventAddressTag())),
             previousReputationEvent),
          previousReputationEvent.getBadgeDefinitionEvent());
  }

  private String calculateReputationEventScore(
     Stream<FormulaEvent> formulaEvents,
     BadgeAwardReputationEvent previousReputationEvent) {
    return formulaEvents
       .map(FormulaEvent::getFormula)
       .reduce(previousReputationEvent.getScore(), ExpressionCalculator::calculate);
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
