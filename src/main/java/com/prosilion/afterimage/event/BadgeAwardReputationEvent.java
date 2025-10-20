package com.prosilion.afterimage.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.prosilion.afterimage.event.internal.Reputation;
import com.prosilion.nostr.event.BadgeAwardAbstractEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import org.springframework.lang.NonNull;

public class BadgeAwardReputationEvent extends BadgeAwardAbstractEvent {
  @JsonIgnore
  @Getter
  PublicKey badgeReceiverPubkey;
  @JsonIgnore
  @Getter
  BadgeDefinitionReputationEvent badgeDefinitionReputationEvent;

  public BadgeAwardReputationEvent(
      @NonNull Identity aImgIdentity,
      @NonNull PublicKey badgeReceiverPubkey,
      @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent,
      @NonNull BigDecimal score) {
    this(aImgIdentity, badgeReceiverPubkey, badgeDefinitionReputationEvent, List.of(), score);
  }

  public BadgeAwardReputationEvent(
      @NonNull Identity aImgIdentity,
      @NonNull PublicKey badgeReceiverPubkey,
      @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent,
      @NonNull List<BaseTag> tags,
      @NonNull BigDecimal score) {
    super(
        aImgIdentity,
        new Reputation(
            badgeReceiverPubkey,
            badgeDefinitionReputationEvent).getAwardEvent(),
        tags,
        score.toString());
  }
}
