package com.prosilion.afterimage.event;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.AbstractBadgeAwardEvent;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.math.BigDecimal;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import lombok.NonNull;

public class ReputationEvent extends AbstractBadgeAwardEvent<KindTypeIF> {
  public ReputationEvent(
      @NonNull Identity aImgIdentity,
      @NonNull PublicKey badgeReceiverPubkey,
      @NonNull BigDecimal score,
      @NonNull URI uri) throws NostrException, NoSuchAlgorithmException {
    super(AfterimageKindType.REPUTATION, aImgIdentity,
        new Reputation(
            aImgIdentity,
            badgeReceiverPubkey,
            AfterimageKindType.REPUTATION).getAwardEvent(),
        score.toString());
  }
}
