package com.prosilion.afterimage.config.util;

import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.superconductor.autoconfigure.base.service.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.autoconfigure.base.service.CacheFormulaEventService;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;

public class DataLoaderRedis implements DataLoaderRedisIF {
  private final CacheServiceIF afterimageCacheService;
  private final CacheFormulaEventService cacheFormulaEventService;
  private final CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService;
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEvent;
  private final BadgeDefinitionAwardEvent badgeDefinitionUpvote;
  private final BadgeDefinitionAwardEvent badgeDefinitionDownvote;

  public DataLoaderRedis(
      @NonNull @Qualifier("redisCacheService") CacheServiceIF afterimageCacheService,
      @NonNull CacheFormulaEventService cacheFormulaEventService,
      @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
      @NonNull BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent,
      @NonNull BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent,
      @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) {
    this.afterimageCacheService = afterimageCacheService;
    this.cacheFormulaEventService = cacheFormulaEventService;
    this.cacheBadgeDefinitionReputationEventService = cacheBadgeDefinitionReputationEventService;
    this.badgeDefinitionUpvote = badgeDefinitionUpvoteEvent;
    this.badgeDefinitionDownvote = badgeDefinitionDownvoteEvent;
    this.badgeDefinitionReputationEvent = badgeDefinitionReputationEvent;
  }

  @Override
  public void run(String... args) {
    afterimageCacheService.save(badgeDefinitionUpvote);
    afterimageCacheService.save(badgeDefinitionDownvote);
    badgeDefinitionReputationEvent.getFormulaEvents().forEach(cacheFormulaEventService::save);
    cacheBadgeDefinitionReputationEventService.save(badgeDefinitionReputationEvent);
  }
}
