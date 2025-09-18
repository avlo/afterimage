package com.prosilion.afterimage.calculator;

import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import org.springframework.lang.NonNull;

public interface ReputationCalculatorIF {
  EventIF calculateUpdatedReputationEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull Optional<EventIF> previousReputationEvent,
      @NonNull EventIF incomingFollowSetsEvent) throws NoSuchAlgorithmException;

  String getFullyQualifiedCalculatorName();
}
