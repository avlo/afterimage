package com.prosilion.afterimage.event;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.internal.Vote;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.util.List;
import org.springframework.lang.NonNull;

public class BadgeAwardDownvoteEvent extends BadgeAwardGenericEvent {
  public BadgeAwardDownvoteEvent(
      @NonNull Identity identity,
      @NonNull PublicKey downvotedUser,
      @NonNull BadgeDefinitionEvent downvoteBadgeDefinitionEvent) throws NostrException {
    this(identity, downvotedUser, downvoteBadgeDefinitionEvent, List.of());
  }

  public BadgeAwardDownvoteEvent(
      @NonNull Identity identity,
      @NonNull PublicKey downvotedUser,
      @NonNull BadgeDefinitionEvent downvoteBadgeDefinitionEvent,
      @NonNull List<BaseTag> tags) throws NostrException {
    super(
        AfterimageKindType.UNIT_DOWNVOTE.getName(),
        identity,
        new Vote(
            downvotedUser, downvoteBadgeDefinitionEvent).getAwardEvent(),
        tags,
        downvoteBadgeDefinitionEvent.getContent());
  }
}
