package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindPlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageFollowSetsEventPlugin extends PublishingEventKindPlugin { // kind 30_000
  private final EventKindTypePluginIF reputationEventPlugin;

  public AfterimageFollowSetsEventPlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull EventKindTypePluginIF reputationEventPlugin) {
    super(notifierService, eventKindPlugin);
    this.reputationEventPlugin = reputationEventPlugin;
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF followSetsEvent) {
    log.debug("{}} processing incoming Kind.FOLLOW_SETS 30_000 : [{}]", getClass().getSimpleName(), followSetsEvent);
/*{
  "kind": 30000,
  "pubkey": "<AIMG_RELAY_PUBKEY>",
  "tags": [
    ["d", "<AimgRepCalculationClass>"],
    ["p", "VOTE_RECIP_1_PUBKEY", "ws://sc.url:port"],

    ["e", "VOTE_EVENT_ID_1", "ws://sc.url:port"],
    ["a", "30009:SC_PUBKEY:upvote"],

    ["e", "VOTE_EVENT_ID_2", "ws://sc.url:port"],
    ["a", "30009:SC_PUBKEY:downvote"],

  "content": current REP score 
}*/
    reputationEventPlugin.processIncomingEvent(followSetsEvent);
  }

  @Override
  public Kind getKind() {
    log.debug("{} getKind of Kind.FOLLOW_SETS 30_000", getClass().getSimpleName());
    return Kind.FOLLOW_SETS; // 30_000
  }
}
