package com.prosilion.afterimage.event.type;

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
public class DownvoteEventKindTypePlugin extends VoteEventKindTypePlugin {

  @Autowired
  public DownvoteEventKindTypePlugin(
      @NonNull AbstractNonPublishingEventKindPlugin abstractNonPublishingEventKindPlugin,
      @NonNull EventEntityService eventEntityService,
      @NonNull ReputationEventKindTypePlugin reputationEventKindTypePlugin,
      @NonNull Identity aImgIdentity,
      @NonNull String afterimageRelayUrl) {
    super(abstractNonPublishingEventKindPlugin, eventEntityService, reputationEventKindTypePlugin, aImgIdentity, afterimageRelayUrl);
    log.debug("DownvoteEventKindTypePlugin loaded");
  }

  @Override
  public KindTypeIF getKindType() {
    log.debug("UpvoteEventKindTypePlugin getKindTypeIF returning AfterimageKindType.DOWNVOTE");
    return AfterimageKindType.DOWNVOTE;
  }
}
