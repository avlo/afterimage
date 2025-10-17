package com.prosilion.afterimage.event;

import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.internal.AwardEvent;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.user.Identity;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Generated;
import org.springframework.lang.NonNull;

public class BadgeAwardGenericEvent extends BaseEvent {
  private final String kindType;

  public BadgeAwardGenericEvent(@NonNull String kindType, @NonNull Identity identity, @NonNull AwardEvent awardEvent, @NonNull String content) throws NostrException {
    super(identity, Kind.BADGE_AWARD_EVENT, Stream.concat(awardEvent.pubkeyTags().stream(), Stream.of(awardEvent.addressTag())).collect(Collectors.toList()), content);
    this.kindType = kindType;
  }

  public BadgeAwardGenericEvent(@NonNull String kindType, @NonNull Identity identity, @NonNull AwardEvent awardEvent, @NonNull List<BaseTag> tags, @NonNull String content) throws NostrException {
    super(identity, Kind.BADGE_AWARD_EVENT, Stream.concat(Stream.concat(tags.stream(), Stream.of(awardEvent.addressTag())), awardEvent.pubkeyTags().stream()).toList(), content);
    this.kindType = kindType;
  }

  @Generated
  public String getKindType() {
    return this.kindType;
  }
}
