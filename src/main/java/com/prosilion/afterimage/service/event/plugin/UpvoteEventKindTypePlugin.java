package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventKindPlugin;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UpvoteEventKindTypePlugin extends VoteEventKindTypePlugin {

  @Autowired
  public UpvoteEventKindTypePlugin(
      @NonNull AbstractNonPublishingEventKindPlugin abstractNonPublishingEventKindPlugin,
      @NonNull EventEntityService eventEntityService,
      @NonNull ReputationEventKindTypePlugin reputationEventKindTypePlugin,
      @NonNull Identity aImgIdentity,
      @NonNull String afterimageRelayUrl) {
    super(abstractNonPublishingEventKindPlugin, eventEntityService, reputationEventKindTypePlugin, aImgIdentity, afterimageRelayUrl);
    log.debug("UpvoteEventKindTypePlugin loaded");
  }

  @Override
  public KindTypeIF getKindType() {
    log.debug("UpvoteEventKindTypePlugin getKindTypeIF returning AfterimageKindType.UPVOTE");
    return AfterimageKindType.UPVOTE;
  }
}
