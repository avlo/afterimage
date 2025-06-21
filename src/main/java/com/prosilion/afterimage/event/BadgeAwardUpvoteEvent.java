package com.prosilion.afterimage.event;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.enums.NostrException;
import com.prosilion.nostr.event.AbstractBadgeAwardEvent;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.NonNull;

public class BadgeAwardUpvoteEvent extends AbstractBadgeAwardEvent<KindTypeIF> {
  private static final Log log = LogFactory.getLog(BadgeAwardUpvoteEvent.class);

  public BadgeAwardUpvoteEvent(
      @NonNull Identity identity,
      @NonNull PublicKey upvotedUser,
      @NonNull String content) throws NostrException, NoSuchAlgorithmException {
    super(AfterimageKindType.UPVOTE, identity,
        new Vote(
            identity.getPublicKey(),
            upvotedUser,
            AfterimageKindType.UPVOTE).getAwardEvent(),
        content);
  }

  public BadgeAwardUpvoteEvent(
      @NonNull Identity identity,
      @NonNull PublicKey upvotedUser,
      @NonNull List<BaseTag> tags,
      @NonNull String content) throws NostrException, NoSuchAlgorithmException {
    super(AfterimageKindType.UPVOTE, identity,
        new Vote(
            identity.getPublicKey(),
            upvotedUser,
            AfterimageKindType.UPVOTE).getAwardEvent(),
        tags,
        content);
  }
}
