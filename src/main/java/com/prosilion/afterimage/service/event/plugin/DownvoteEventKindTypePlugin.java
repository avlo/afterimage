package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.superconductor.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class DownvoteEventKindTypePlugin extends VoteEventKindTypePlugin {

  public DownvoteEventKindTypePlugin(
      @NonNull EventEntityService eventEntityService,
      @NonNull EventKindTypePluginIF<KindTypeIF> reputationEventKindTypePlugin) {
    super(eventEntityService, reputationEventKindTypePlugin);
    log.debug("DownvoteEventKindTypePlugin loaded");
  }

  @Override
  public KindTypeIF getKindType() {
    log.debug("DownvoteEventKindTypePlugin getKindTypeIF returning AfterimageKindType.DOWNVOTE");
    return AfterimageKindType.DOWNVOTE;
  }
}
