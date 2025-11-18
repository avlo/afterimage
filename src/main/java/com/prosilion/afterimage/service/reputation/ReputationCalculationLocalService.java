package com.prosilion.afterimage.service.reputation;

import com.prosilion.afterimage.calculator.ReputationCalculatorIF;
import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.user.PublicKey;
import java.util.List;
import org.springframework.lang.NonNull;

public class ReputationCalculationLocalService implements ReputationCalculationServiceIF {
  ReputationCalculatorIF reputationCalculatorIF;

  public ReputationCalculationLocalService(@NonNull ReputationCalculatorIF reputationCalculatorIF) {
    this.reputationCalculatorIF = reputationCalculatorIF;
  }

  @Override
  public EventIF calculateReputationEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull BadgeAwardReputationEvent previousReputationEvent,
      @NonNull List<FormulaEvent> formulaEvents,
      @NonNull EventIF incomingFollowSetsEvent) {
    return reputationCalculatorIF.calculateUpdatedReputationEvent(voteReceiverPubkey, previousReputationEvent, formulaEvents, incomingFollowSetsEvent);
  }
}
