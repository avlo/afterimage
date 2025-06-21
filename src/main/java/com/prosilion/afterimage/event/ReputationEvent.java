package com.prosilion.afterimage.event;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.enums.NostrException;
import com.prosilion.nostr.event.AbstractBadgeAwardEvent;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.math.BigDecimal;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import lombok.NonNull;

public class ReputationEvent extends AbstractBadgeAwardEvent<KindTypeIF> {
  public ReputationEvent(
      @NonNull Identity identity,
      @NonNull PublicKey badgeReceiverPubkey,
      @NonNull BigDecimal score,
      @NonNull URI uri) throws NostrException, NoSuchAlgorithmException {
    super(AfterimageKindType.REPUTATION, identity,
        new Reputation(
            identity.getPublicKey(),
            badgeReceiverPubkey,
            AfterimageKindType.REPUTATION).getAwardEvent(),
        score.toString());
  }
}
