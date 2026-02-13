package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.cache.CacheBadgeAwardGenericEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheBadgeDefinitionGenericEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheBadgeDefinitionReputationEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheFormulaEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class UniversalVoteEventPlugin extends AbstractVoteEventPlugin {
  public UniversalVoteEventPlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull CacheBadgeDefinitionGenericEventServiceIF cacheBadgeDefinitionGenericEventServiceIF,
      @NonNull CacheFormulaEventServiceIF cacheFormulaEventServiceIF,
      @NonNull CacheBadgeAwardGenericEventServiceIF<BadgeDefinitionGenericEvent, BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> cacheBadgeAwardGenericEventServiceIF,
      @NonNull CacheBadgeDefinitionReputationEventServiceIF cacheBadgeDefinitionReputationEventServiceIF,
      @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
      @NonNull AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin,
      @NonNull EventKindPluginIF eventKindPluginIF,
      @NonNull Identity aImgIdentity) {
    super(
        afterimageRelayUrl,
        cacheServiceIF,
        cacheBadgeDefinitionGenericEventServiceIF,
        cacheFormulaEventServiceIF,
        cacheBadgeDefinitionReputationEventServiceIF,
        cacheBadgeAwardGenericEventServiceIF,
        cacheFollowSetsEventServiceIF,
        afterimageFollowSetsEventPlugin,
        eventKindPluginIF,
        aImgIdentity);
  }
}
