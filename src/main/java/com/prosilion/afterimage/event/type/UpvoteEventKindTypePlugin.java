package com.prosilion.afterimage.event.type;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventKindPlugin;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class UpvoteEventKindTypePlugin extends VoteEventKindTypePlugin {

  @Autowired
  public UpvoteEventKindTypePlugin(
      @NonNull AbstractNonPublishingEventKindPlugin abstractNonPublishingEventKindPlugin,
      @NonNull EventEntityService eventEntityService,
      @NonNull ReputationEventKindTypePlugin reputationEventKindTypePlugin,
      @NonNull Identity aImgIdentity) {
    super(abstractNonPublishingEventKindPlugin, eventEntityService, reputationEventKindTypePlugin, aImgIdentity);
  }

  @Override
  public KindTypeIF getKindType() {
    return AfterimageKindType.UPVOTE;
  }
}
