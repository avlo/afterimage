package com.prosilion.afterimage.config;

import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.superconductor.autoconfigure.redis.config.DataLoaderRedisIF;
import com.prosilion.superconductor.base.service.event.type.EventPluginIF;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;

public class DataLoaderRedis implements DataLoaderRedisIF {
  private final EventPluginIF eventPlugin;
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEvent;
  private final BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent;
  private final BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent;

  public DataLoaderRedis(
      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
      @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent,
      @NonNull BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent,
      @NonNull BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent) {
    this.eventPlugin = eventPlugin;
    this.badgeDefinitionReputationEvent = badgeDefinitionReputationEvent;
    this.badgeDefinitionUpvoteEvent = badgeDefinitionUpvoteEvent;
    this.badgeDefinitionDownvoteEvent = badgeDefinitionDownvoteEvent;
  }

  @Override
  public void run(String... args) {
    eventPlugin.processIncomingEvent(badgeDefinitionReputationEvent);
    eventPlugin.processIncomingEvent(badgeDefinitionUpvoteEvent);
    eventPlugin.processIncomingEvent(badgeDefinitionDownvoteEvent);
  }
}
