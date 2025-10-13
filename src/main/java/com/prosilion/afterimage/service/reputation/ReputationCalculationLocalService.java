package com.prosilion.afterimage.service.reputation;

import com.prosilion.afterimage.calculator.ReputationCalculatorIF;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.user.PublicKey;
import java.util.Optional;
import org.springframework.lang.NonNull;

public class ReputationCalculationLocalService implements ReputationCalculationServiceIF {
  ReputationCalculatorIF reputationCalculatorIF;

  public ReputationCalculationLocalService(@NonNull ReputationCalculatorIF reputationCalculatorIF) {
    this.reputationCalculatorIF = reputationCalculatorIF;
  }

  @Override
  public EventIF calculateReputationEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull Optional<EventIF> previousReputationEvent,
      @NonNull EventIF incomingFollowSetsEvent) {
    return reputationCalculatorIF.calculateUpdatedReputationEvent(voteReceiverPubkey, previousReputationEvent, incomingFollowSetsEvent);
  }
}
