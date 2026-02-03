package com.prosilion.afterimage.config.util;

import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;

public class DataLoaderRedis implements DataLoaderRedisIF {
  private final CacheServiceIF afterimageCacheService;
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEvent;
  private final BadgeDefinitionGenericEvent badgeDefinitionUpvote;
  private final BadgeDefinitionGenericEvent badgeDefinitionDownvote;

  public DataLoaderRedis(
      @NonNull @Qualifier("redisCacheService") CacheServiceIF afterimageCacheService,
      @NonNull BadgeDefinitionGenericEvent badgeDefinitionUpvoteEvent,
      @NonNull BadgeDefinitionGenericEvent badgeDefinitionDownvoteEvent,
      @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) {
    this.afterimageCacheService = afterimageCacheService;
    this.badgeDefinitionUpvote = badgeDefinitionUpvoteEvent;
    this.badgeDefinitionDownvote = badgeDefinitionDownvoteEvent;
    this.badgeDefinitionReputationEvent = badgeDefinitionReputationEvent;
  }

  @Override
  public void run(String... args) {
    afterimageCacheService.save(badgeDefinitionUpvote);
    afterimageCacheService.save(badgeDefinitionDownvote);
    badgeDefinitionReputationEvent.getFormulaEvents().forEach(afterimageCacheService::save);
    afterimageCacheService.save(badgeDefinitionReputationEvent);
  }
}
