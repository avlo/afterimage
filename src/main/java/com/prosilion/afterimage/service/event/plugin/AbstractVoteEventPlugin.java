package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.NonPublishingEventKindTypePlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
// our SportsCar extends CarDecorator
public abstract class AbstractVoteEventPlugin extends NonPublishingEventKindTypePlugin {
  private final EventKindPluginIF afterimageFollowSetsEventPlugin;
//  private final Identity aImgIdentity;
//  private final Relay relay;

  public AbstractVoteEventPlugin(
//      @NonNull String afterimageRelayUrl,
      @NonNull EventKindTypePluginIF eventKindTypePluginIF,
      @NonNull EventKindPluginIF afterimageFollowSetsEventPlugin
//      @NonNull Identity aImgIdentity
  ) {
    super(eventKindTypePluginIF);
//    this.relay = new Relay(afterimageRelayUrl);
    this.afterimageFollowSetsEventPlugin = afterimageFollowSetsEventPlugin;
//    this.aImgIdentity = aImgIdentity;
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF voteEvent) {
    log.debug("{} processing incoming VOTE EVENT: [{}]", getClass().getSimpleName(), voteEvent);
    afterimageFollowSetsEventPlugin.processIncomingEvent(
        voteEvent);
  }

//  @SneakyThrows
//  private EventIF createFollowSetsEvent(
//      @NonNull PublicKey voteReceiverPubkey,
//      @NonNull EventTag eventTag) {
//    return new FollowSetsEvent(
//        aImgIdentity,
//        voteReceiverPubkey,
//        new IdentifierTag(
//            UNIT_REPUTATION.getName()),
//        relay,
//        List.of(eventTag),

  /// /        TODO: replace content
//        "Follow Sets Event Content: available for useful use").getGenericEventRecord();
//  }
  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }
}
