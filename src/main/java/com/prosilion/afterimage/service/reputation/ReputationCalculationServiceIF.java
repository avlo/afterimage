package com.prosilion.afterimage.service.reputation;

import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.user.PublicKey;
import org.springframework.lang.NonNull;

public interface ReputationCalculationServiceIF {
  EventIF calculateReputationEvent(
      PublicKey voteReceiverPubkey,
      BadgeAwardReputationEvent previousReputationEvent,
      EventIF incomingFollowSetsEvent);
}
