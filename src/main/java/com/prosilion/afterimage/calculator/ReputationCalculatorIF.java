package com.prosilion.afterimage.calculator;

import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.user.PublicKey;

public interface ReputationCalculatorIF {
  EventIF calculateUpdatedReputationEvent(
      PublicKey voteReceiverPubkey,
      BadgeAwardReputationEvent dbPreviousReputationEvent,
      EventIF incomingFollowSetsEvent);

  String getFullyQualifiedCalculatorName();
}
