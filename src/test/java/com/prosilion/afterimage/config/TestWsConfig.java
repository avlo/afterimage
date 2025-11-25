package com.prosilion.afterimage.config;

import com.prosilion.afterimage.config.util.DataLoaderRedis;
import com.prosilion.afterimage.config.util.DataLoaderRedisIF;
import com.prosilion.afterimage.db.AfterimageCacheService;
import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class TestWsConfig {
  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public AfterimageMeshRelayService afterimageMeshRelayService(String afterimageRelayUrl) {
    return new AfterimageMeshRelayService(afterimageRelayUrl);
  }

  @Bean
  DataLoaderRedisIF dataLoaderRedis(
      AfterimageCacheService afterimageCacheService,
      @Qualifier("badgeDefinitionUpvoteEvent") BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent,
      @Qualifier("badgeDefinitionDownvoteEvent") BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent,
      BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) {
    return new DataLoaderRedis(
        afterimageCacheService,
        badgeDefinitionUpvoteEvent,
        badgeDefinitionDownvoteEvent,
        badgeDefinitionReputationEvent);
  }
}
