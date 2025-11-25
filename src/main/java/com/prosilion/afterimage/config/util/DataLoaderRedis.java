package com.prosilion.afterimage.config.util;

import com.prosilion.afterimage.db.AfterimageCacheService;
import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import org.springframework.lang.NonNull;

public class DataLoaderRedis implements DataLoaderRedisIF {
  private final AfterimageCacheService afterimageCacheService;
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEvent;
  private final BadgeDefinitionAwardEvent badgeDefinitionUpvote;
  private final BadgeDefinitionAwardEvent badgeDefinitionDownvote;

  public 
  DataLoaderRedis(
      @NonNull AfterimageCacheService afterimageCacheService,
      @NonNull BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent,
      @NonNull BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent,
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
