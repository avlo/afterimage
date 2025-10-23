package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class UniversalVoteEventPlugin extends AbstractVoteEventPlugin {

  public UniversalVoteEventPlugin(
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull EventKindPluginIF afterimageFollowSetsEventPlugin,
      @NonNull Identity aImgIdentity) {
    super(eventKindPlugin, afterimageFollowSetsEventPlugin, aImgIdentity);
    log.debug("{} loaded", getClass().getSimpleName());
  }
}
