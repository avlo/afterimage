package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FollowSetsEvent.EventTagAddressTagPair;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.lang.NonNull;

public interface FollowSetsCreatorPlugin {
  default <T extends BaseTag> T getTypeSpecificTags(Class<T> clazz, EventIF eventIf) {
    return Filterable.getTypeSpecificTags(clazz, eventIf)
        .stream()
        .findFirst().orElseThrow();
  }

  @SneakyThrows
  default EventIF createFollowSetsEvent(
      @NonNull Identity aImgIdentity,
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull IdentifierTag calculatorUuid,
      @NonNull EventTagAddressTagPair... eventTagAddressTagPairs) {
    return createFollowSetsEvent(
        aImgIdentity,
        voteReceiverPubkey,
        calculatorUuid,
        List.of(eventTagAddressTagPairs));
  }

  @SneakyThrows
  default EventIF createFollowSetsEvent(
      @NonNull Identity aImgIdentity,
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull IdentifierTag calculatorUuid,
      @NonNull List<EventTagAddressTagPair> eventTagAddressTagPairs) {
    return new FollowSetsEvent(
        aImgIdentity,
        voteReceiverPubkey,
        calculatorUuid,
        eventTagAddressTagPairs,
        String.format("FollowSets event created by %s", getClass().getSimpleName())
//        TODO: potentially remove getgenericeventrecord
    ).getGenericEventRecord();
  }
}
