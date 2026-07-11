package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.base.cache.CacheCurationSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheFormulaEventServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UniversalVoteEventKindPlugin extends AbstractVoteEventKindPlugin {
  public UniversalVoteEventKindPlugin(
     @NonNull String afterimageRelayUrl,
     @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
     @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull AfterimageFollowSetsEventKindPlugin afterimageFollowSetsEventKindPlugin,
     @NonNull CacheFormulaEventServiceIF cacheFormulaEventServiceIF,
     @NonNull CacheCurationSetsEventServiceIF cacheCurationSetsEventServiceIF,
     @NonNull EventPlugin eventPlugin,
     @NonNull Identity aImgIdentity) {
    super(
       afterimageRelayUrl,
       cacheBadgeDefinitionGenericEventService,
       cacheBadgeDefinitionReputationEventService,
       cacheFollowSetsEventService,
       afterimageFollowSetsEventKindPlugin,
       cacheFormulaEventServiceIF,
       cacheCurationSetsEventServiceIF,
       eventPlugin,
       aImgIdentity);
  }
}
