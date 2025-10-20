package com.prosilion.afterimage.event.internal;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
import com.prosilion.nostr.event.internal.AwardEvent;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.PublicKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.lang.NonNull;

@Getter
@EqualsAndHashCode(callSuper = false)
public class Vote {
  private final AwardEvent awardEvent;

  public Vote(@NonNull PublicKey upvotedUser, @NonNull BadgeDefinitionAwardEvent badgeAwardDefinitionEvent) {
    awardEvent = new AwardEvent(
        new AddressTag(
            Kind.BADGE_DEFINITION_EVENT,
            badgeAwardDefinitionEvent.getPublicKey(),
            badgeAwardDefinitionEvent.getIdentifierTag()),
        new PubKeyTag(upvotedUser));
  }
}

