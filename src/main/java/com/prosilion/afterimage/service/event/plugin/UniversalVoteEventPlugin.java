package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class UniversalVoteEventPlugin extends AbstractVoteEventPlugin {
  public UniversalVoteEventPlugin(
      @NonNull EventKindTypePluginIF eventKindTypePluginIF,
      @NonNull EventKindPluginIF afterimageFollowSetsEventPlugin,
      @NonNull Identity aImgIdentity) {
    super(eventKindTypePluginIF, afterimageFollowSetsEventPlugin, aImgIdentity);
    log.debug("{} loaded", getClass().getSimpleName());
  }
}
