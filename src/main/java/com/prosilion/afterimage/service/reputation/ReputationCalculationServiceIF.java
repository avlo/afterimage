package com.prosilion.afterimage.service.reputation;

import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.user.PublicKey;
import java.util.List;

public interface ReputationCalculationServiceIF {
  EventIF calculateReputationEvent(
      PublicKey voteReceiverPubkey,
      BadgeAwardReputationEvent previousReputationEvent,
      List<FormulaEvent> formulaEvents,
      EventIF incomingFollowSetsEvent);
}
