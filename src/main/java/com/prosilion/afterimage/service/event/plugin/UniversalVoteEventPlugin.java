package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class UniversalVoteEventPlugin extends AbstractVoteEventPlugin {
  public UniversalVoteEventPlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
      @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
      @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
      @NonNull AfterimageFollowSetsEventKindPlugin afterimageFollowSetsEventKindPlugin,
      @NonNull EventPlugin eventPlugin,
      @NonNull Identity aImgIdentity) {
    super(
        afterimageRelayUrl,
        cacheServiceIF,
        cacheBadgeDefinitionGenericEventService,
        cacheBadgeDefinitionReputationEventService,
        cacheFollowSetsEventService,
        afterimageFollowSetsEventKindPlugin,
        eventPlugin,
        aImgIdentity);
  }
}
