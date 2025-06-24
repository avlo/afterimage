package com.prosilion.afterimage.event;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.enums.NostrException;
import com.prosilion.nostr.event.AbstractBadgeAwardEvent;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.user.Identity;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.springframework.lang.NonNull;

public class BadgeAwardDownvoteEvent extends AbstractBadgeAwardEvent<KindTypeIF> {
  private final static String DOWNVOTE_CONTENT = "-1";

  public BadgeAwardDownvoteEvent(
      @NonNull Identity identity,
      @NonNull Identity upvotedUser) throws NostrException, NoSuchAlgorithmException {
    super(AfterimageKindType.DOWNVOTE, identity,
        new Vote(
            identity.getPublicKey(),
            upvotedUser.getPublicKey(),
            AfterimageKindType.DOWNVOTE).getAwardEvent(),
        DOWNVOTE_CONTENT);
  }

  public BadgeAwardDownvoteEvent(
      @NonNull Identity identity,
      @NonNull Identity upvotedUser,
      @NonNull List<BaseTag> tags) throws NostrException, NoSuchAlgorithmException {
    super(AfterimageKindType.DOWNVOTE, identity,
        new Vote(
            identity.getPublicKey(),
            upvotedUser.getPublicKey(),
            AfterimageKindType.DOWNVOTE).getAwardEvent(),
        tags,
        DOWNVOTE_CONTENT);
  }
}
