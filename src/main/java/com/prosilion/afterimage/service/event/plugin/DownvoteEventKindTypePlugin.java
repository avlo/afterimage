package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class DownvoteEventKindTypePlugin extends VoteEventKindTypePlugin {

  public DownvoteEventKindTypePlugin(
      @NonNull EventKindTypePluginIF<KindTypeIF> eventKindTypePlugin,
      @NonNull EventKindTypePluginIF<KindTypeIF> reputationEventKindTypePlugin) {
    super(eventKindTypePlugin, reputationEventKindTypePlugin);
    log.debug("DownvoteEventKindTypePlugin loaded");
  }

  @Override
  public KindTypeIF getKindType() {
    log.debug("DownvoteEventKindTypePlugin getKindTypeIF returning SuperconductorKindType.DOWNVOTE");
    return SuperconductorKindType.DOWNVOTE;
  }
}
