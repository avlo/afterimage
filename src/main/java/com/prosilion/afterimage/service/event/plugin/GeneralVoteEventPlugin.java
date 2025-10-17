package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class GeneralVoteEventPlugin extends AbstractGeneralVoteEventPlugin {

  public GeneralVoteEventPlugin(
      @NonNull EventKindPluginIF afterimageFollowSetsEventPlugin,
      @NonNull Identity aImgIdentity) {
    super(afterimageFollowSetsEventPlugin, aImgIdentity);
    log.debug("AbstractGeneralVoteEventPlugin loaded");
  }
}
