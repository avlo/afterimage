package com.prosilion.afterimage.config;

import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.superconductor.autoconfigure.redis.config.DataLoaderRedisIF;
import com.prosilion.superconductor.base.service.event.type.EventPluginIF;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;

public class DataLoaderRedis implements DataLoaderRedisIF {
  private final EventPluginIF eventPlugin;
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEvent;

  public DataLoaderRedis(
      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
      @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) {
    this.eventPlugin = eventPlugin;
    this.badgeDefinitionReputationEvent = badgeDefinitionReputationEvent;
  }

  @Override
  public void run(String... args) {
    eventPlugin.processIncomingEvent(badgeDefinitionReputationEvent);
  }
}
