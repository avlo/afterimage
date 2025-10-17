package com.prosilion.afterimage.util.event;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.util.List;
import org.springframework.lang.NonNull;

public class BadgeAwardUpvoteEvent extends BadgeAwardGenericEvent {
  public BadgeAwardUpvoteEvent(
      @NonNull Identity authorIdentity,
      @NonNull PublicKey upvotedUser,
      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent) throws NostrException {
    this(authorIdentity, upvotedUser, upvoteBadgeDefinitionEvent, List.of());
  }

  public BadgeAwardUpvoteEvent(
      @NonNull Identity identity,
      @NonNull PublicKey upvotedUser,
      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent,
      @NonNull List<BaseTag> tags) throws NostrException {
    super(
        AfterimageKindType.UNIT_UPVOTE.getName(),
        identity,
        new Vote(upvotedUser, upvoteBadgeDefinitionEvent).getAwardEvent(),
        tags,
        upvoteBadgeDefinitionEvent.getContent());
  }

}
