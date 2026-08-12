package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.curated.CacheCuratedBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.curated.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.base.cache.curated.CacheCuratedBadgeAwardEventServiceIF;
import com.prosilion.superconductor.base.cache.curated.CacheCuratedFormulaEventServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.lib.redis.service.RedisCacheService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UniversalVoteEventKindPlugin extends AbstractVoteEventKindPlugin {
  public UniversalVoteEventKindPlugin(
     @NonNull RedisCacheService redisCacheService,
     @NonNull String afterimageRelayUrl,
     @NonNull CacheCuratedBadgeAwardEventServiceIF cacheCuratedBadgeAwardEventServiceIF,
     @NonNull CacheCuratedBadgeDefinitionGenericEventService cacheCuratedBadgeDefinitionGenericEventService,
     @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull AfterimageFollowSetsEventKindPlugin afterimageFollowSetsEventKindPlugin,
     @NonNull CacheCuratedFormulaEventServiceIF cacheCuratedFormulaEventServiceIF,
     @NonNull EventPlugin eventPlugin,
     @NonNull Identity aImgIdentity) {
    super(
       redisCacheService,
       afterimageRelayUrl,
       cacheCuratedBadgeAwardEventServiceIF,
       cacheCuratedBadgeDefinitionGenericEventService,
       cacheBadgeDefinitionReputationEventService,
       cacheFollowSetsEventService,
       afterimageFollowSetsEventKindPlugin,
       cacheCuratedFormulaEventServiceIF,
       eventPlugin,
       aImgIdentity);
  }
}
