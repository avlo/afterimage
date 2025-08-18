package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.NonPublishingEventKindTypePlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
// our SportsCar extends CarDecorator
public abstract class VoteEventKindTypePlugin extends NonPublishingEventKindTypePlugin {
  private final EventKindTypePluginIF reputationEventKindTypePlugin;

  public VoteEventKindTypePlugin(
      @NonNull EventKindTypePluginIF eventKindTypePlugin,
      @NonNull EventKindTypePluginIF reputationEventKindTypePlugin) {
    super(eventKindTypePlugin);
    this.reputationEventKindTypePlugin = reputationEventKindTypePlugin;
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF voteEvent) {
    log.debug("VoteEventKindTypePlugin processing incoming VOTE EVENT: [{}]", voteEvent);
//    saves VOTE event without triggering subscriber listener
    super.processIncomingEvent(voteEvent);
    log.debug("vote saved to db, send vote off to reputationEventKindTypePlugin for rep calculation");
    reputationEventKindTypePlugin.processIncomingEvent(voteEvent);
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }

  abstract public KindTypeIF getKindType();
}
