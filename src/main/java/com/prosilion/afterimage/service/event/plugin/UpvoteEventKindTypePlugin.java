package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class UpvoteEventKindTypePlugin extends VoteEventKindTypePlugin {

  public UpvoteEventKindTypePlugin(
      @NonNull EventEntityService eventEntityService,
      @NonNull ReputationEventKindTypePlugin reputationEventKindTypePlugin,
      @NonNull Identity aImgIdentity,
      @NonNull String afterimageRelayUrl) {
    super(eventEntityService, reputationEventKindTypePlugin, aImgIdentity, afterimageRelayUrl);
    log.debug("UpvoteEventKindTypePlugin loaded");
  }

  @Override
  public KindTypeIF getKindType() {
    log.debug("UpvoteEventKindTypePlugin getKindTypeIF returning AfterimageKindType.UPVOTE");
    return AfterimageKindType.UPVOTE;
  }
}
