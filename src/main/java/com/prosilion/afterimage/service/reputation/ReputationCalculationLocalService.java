package com.prosilion.afterimage.service.reputation;

import com.prosilion.afterimage.calculator.ReputationCalculatorIF;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.user.PublicKey;
import java.util.List;
import org.springframework.lang.NonNull;

public class ReputationCalculationLocalService implements ReputationCalculationServiceIF {
  ReputationCalculatorIF reputationCalculator;

  public ReputationCalculationLocalService(@NonNull ReputationCalculatorIF reputationCalculator) {
    this.reputationCalculator = reputationCalculator;
  }

  @Override
  public BadgeAwardReputationEvent calculateReputationEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull BadgeAwardReputationEvent previousReputationEvent,
      @NonNull List<FormulaEvent> formulaEvents,
      @NonNull FollowSetsEvent incomingFollowSetsEvent) {
    return reputationCalculator.calculateUpdatedReputationEvent(voteReceiverPubkey, previousReputationEvent, formulaEvents, incomingFollowSetsEvent);
  }
}
