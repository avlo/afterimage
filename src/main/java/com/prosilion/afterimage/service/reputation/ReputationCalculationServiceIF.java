package com.prosilion.afterimage.service.reputation;

import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.user.PublicKey;
import java.util.Optional;
import org.springframework.lang.NonNull;

public interface ReputationCalculationServiceIF {
  EventIF calculateReputationEvent(
      @NonNull PublicKey voteReceiverPubkey,
      Optional<EventIF> previousReputationEvent,
      EventIF incomingFollowSetsEvent);
}
