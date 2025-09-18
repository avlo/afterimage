package com.prosilion.afterimage.event;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.internal.Reputation;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.AbstractBadgeAwardEvent;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.springframework.lang.NonNull;

public class BadgeAwardReputationEvent extends AbstractBadgeAwardEvent<KindTypeIF> {

  public BadgeAwardReputationEvent(
      @NonNull Identity aImgIdentity,
      @NonNull PublicKey badgeReceiverPubkey,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent,
      @NonNull List<BaseTag> tags,
      @NonNull BigDecimal score) throws NostrException, NoSuchAlgorithmException {
    super(AfterimageKindType.REPUTATION,
        aImgIdentity,
        new Reputation(
            badgeReceiverPubkey,
            reputationBadgeDefinitionEvent).getAwardEvent(),
        tags,
        score.toString());
  }
}
