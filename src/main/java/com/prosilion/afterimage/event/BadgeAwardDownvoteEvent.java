package com.prosilion.afterimage.event;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.enums.NostrException;
import com.prosilion.nostr.event.AbstractBadgeAwardEvent;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.user.Identity;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.NonNull;

public class BadgeAwardDownvoteEvent extends AbstractBadgeAwardEvent<KindTypeIF> {
  private static final Log log = LogFactory.getLog(BadgeAwardDownvoteEvent.class);

  public BadgeAwardDownvoteEvent(
      @NonNull Identity identity,
      @NonNull Identity upvotedUser,
      @NonNull String content) throws NostrException, NoSuchAlgorithmException {
    super(AfterimageKindType.DOWNVOTE, identity,
        new Vote(
            identity.getPublicKey(),
            upvotedUser.getPublicKey(),
            AfterimageKindType.DOWNVOTE).getAwardEvent(),
        content);
  }

  public BadgeAwardDownvoteEvent(
      @NonNull Identity identity,
      @NonNull Identity upvotedUser,
      @NonNull List<BaseTag> tags,
      @NonNull String content) throws NostrException, NoSuchAlgorithmException {
    super(AfterimageKindType.DOWNVOTE, identity,
        new Vote(
            identity.getPublicKey(),
            upvotedUser.getPublicKey(),
            AfterimageKindType.DOWNVOTE).getAwardEvent(),
        tags,
        content);
  }
}
