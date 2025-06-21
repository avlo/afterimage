package com.prosilion.afterimage.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AfterimageKindType implements KindTypeIF {
  UPVOTE(Kind.BADGE_AWARD_EVENT, "upvote"),
  DOWNVOTE(Kind.BADGE_AWARD_EVENT, "downvote"),
  REPUTATION(Kind.BADGE_AWARD_EVENT, "reputation");

  private final Kind kind;

  @JsonValue
  private final String name;
}
