package com.prosilion.afterimage.service.reputation;

import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.user.PublicKey;
import java.util.List;
import java.util.Optional;

public interface ReputationCalculationServiceIF {
  Optional<BadgeAwardReputationEvent> calculateReputationEvent(
     PublicKey voteReceiverPubkey,
     Optional<BadgeAwardReputationEvent> previousReputationEvent,
     List<FormulaEvent> formulaEvents,
     FollowSetsEvent incomingFollowSetsEvent);
}
