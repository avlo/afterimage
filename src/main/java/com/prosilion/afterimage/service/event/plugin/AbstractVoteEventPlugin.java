package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FollowSetsEvent.EventTagAddressTagPair;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.NonPublishingEventKindTypePlugin;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
// our SportsCar extends CarDecorator
public abstract class AbstractVoteEventPlugin extends NonPublishingEventKindTypePlugin {
  private final EventKindPluginIF afterimageFollowSetsEventPlugin;
  private final Identity aImgIdentity;

  public AbstractVoteEventPlugin(
      @NonNull EventKindTypePluginIF eventKindTypePlugin,
      @NonNull EventKindPluginIF afterimageFollowSetsEventPlugin,
      @NonNull Identity aImgIdentity) {
    super(eventKindTypePlugin);
    this.afterimageFollowSetsEventPlugin = afterimageFollowSetsEventPlugin;
    this.aImgIdentity = aImgIdentity;
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF voteEvent) {
    log.debug("VoteEventKindTypePlugin processing incoming VOTE EVENT: [{}]", voteEvent);

    PublicKey voteReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, voteEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

//    saves VOTE event without triggering subscriber listener
//    TODO: since below vote will now only get added to FollowSets during reputationEventPlugin (below), no longer required to save it.  
//          thus, revisit use of extending NonPublishingEventKindTypePlugin 
//    super.processIncomingEvent(voteEvent);
//    log.debug("vote saved to db");

    AddressTag addressTag = Filterable.getTypeSpecificTags(AddressTag.class, voteEvent)
        .stream()
        .findFirst().orElseThrow();

    EventTagAddressTagPair eventTagAddressTagPair = new EventTagAddressTagPair(
        new EventTag(voteEvent.getId()),
        addressTag);

    EventIF newFollowSetsEvent = createFollowSetsEvent(
        voteReceiverPubkey,
        eventTagAddressTagPair);

    log.debug("send vote to reputationEventPlugin for rep calculation");
    afterimageFollowSetsEventPlugin.processIncomingEvent(newFollowSetsEvent);
  }

  @SneakyThrows
  private EventIF createFollowSetsEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull EventTagAddressTagPair eventTagAddressTagPairs) {

    return new FollowSetsEvent(
        aImgIdentity,
        voteReceiverPubkey,
        List.of(eventTagAddressTagPairs),
//        TODO: replace 999999
        "99999").getGenericEventRecord();
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }

  abstract public KindTypeIF getKindType();
}
