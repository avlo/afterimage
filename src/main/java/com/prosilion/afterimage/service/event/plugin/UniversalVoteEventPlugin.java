package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.CacheBadgeAwardGenericEventServiceIF;
import com.prosilion.superconductor.base.service.CacheBadgeDefinitionGenericEventServiceIF;
import com.prosilion.superconductor.base.service.CacheBadgeDefinitionReputationEventServiceIF;
import com.prosilion.superconductor.base.service.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.service.CacheFormulaEventServiceIF;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class UniversalVoteEventPlugin extends AbstractVoteEventPlugin {
  public UniversalVoteEventPlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull CacheBadgeAwardGenericEventServiceIF cacheBadgeAwardGenericEventServiceIF,
      @NonNull CacheBadgeDefinitionGenericEventServiceIF cacheBadgeDefinitionGenericEventServiceIF,
      @NonNull CacheFormulaEventServiceIF cacheFormulaEventServiceIF,
      @NonNull CacheBadgeDefinitionReputationEventServiceIF cacheBadgeDefinitionReputationEventServiceIF,
      @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
      @NonNull AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin,
      @NonNull EventKindPluginIF eventKindPluginIF,
      @NonNull Identity aImgIdentity) {
    super(
        afterimageRelayUrl,
        cacheServiceIF,
        cacheBadgeAwardGenericEventServiceIF,
        cacheBadgeDefinitionGenericEventServiceIF,
        cacheFormulaEventServiceIF,
        cacheBadgeDefinitionReputationEventServiceIF,
        cacheFollowSetsEventServiceIF,
        afterimageFollowSetsEventPlugin,
        eventKindPluginIF,
        aImgIdentity);
    log.debug("{} loaded", getClass().getSimpleName());
  }
}
