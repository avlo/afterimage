package com.prosilion.afterimage.config;

import com.prosilion.afterimage.db.AfterimageCacheService;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import org.springframework.lang.NonNull;

public class DataLoaderRedis implements DataLoaderRedisIF {
  private final AfterimageCacheService afterimageCacheService;
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEvent;

  public DataLoaderRedis(
      @NonNull AfterimageCacheService afterimageCacheService,
      @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) {
    this.afterimageCacheService = afterimageCacheService;
    this.badgeDefinitionReputationEvent = badgeDefinitionReputationEvent;
  }

  @Override
  public void run(String... args) {
    afterimageCacheService.save(badgeDefinitionReputationEvent);
  }
}
