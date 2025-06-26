package com.prosilion.afterimage.event.internal;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.internal.AwardEvent;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.lang.NonNull;

@Getter
@EqualsAndHashCode(callSuper = false)
public class Reputation {
  private final AwardEvent awardEvent;

  public Reputation(@NonNull Identity aImgIdentity, @NonNull PublicKey upvotedUser, @NonNull KindTypeIF voteKindType) {
    IdentifierTag identifierTag = new IdentifierTag(voteKindType.getName());
    AddressTag addressTag = new AddressTag(
        Kind.BADGE_DEFINITION_EVENT,
        aImgIdentity.getPublicKey(),
        identifierTag);

    awardEvent = new AwardEvent(addressTag, new PubKeyTag(upvotedUser));
  }
}

