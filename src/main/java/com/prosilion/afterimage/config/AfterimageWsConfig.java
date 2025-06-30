package com.prosilion.afterimage.config;

import com.prosilion.afterimage.relay.AfterimageMeshRelayService;
import com.prosilion.afterimage.service.event.plugin.DownvoteEventKindTypePlugin;
import com.prosilion.afterimage.service.event.plugin.ReputationEventKindTypePlugin;
import com.prosilion.afterimage.service.event.plugin.SuperConductorRelayEnlistmentNonPublishingEventKindPlugin;
import com.prosilion.afterimage.service.event.plugin.UpvoteEventKindTypePlugin;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.NonPublishingEventKindPlugin;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.NonNull;

@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class AfterimageWsConfig extends AfterimageBaseConfig {
  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public AfterimageMeshRelayService afterimageReactiveRelayClient(@NonNull String afterimageRelayUrl) {
    return new AfterimageMeshRelayService(afterimageRelayUrl);
  }

  @Bean
  EventKindTypePluginIF<KindTypeIF> upvoteEventKindTypePlugin(
      @NonNull EventEntityService eventEntityService,
      @NonNull ReputationEventKindTypePlugin reputationEventKindTypePlugin,
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull String afterimageRelayUrl) {
    return new UpvoteEventKindTypePlugin(
        eventEntityService,
        reputationEventKindTypePlugin,
        afterimageInstanceIdentity,
        afterimageRelayUrl);
  }

  @Bean
  EventKindTypePluginIF<KindTypeIF> downvoteEventKindTypePlugin(
      @NonNull EventEntityService eventEntityService,
      @NonNull ReputationEventKindTypePlugin reputationEventKindTypePlugin,
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull String afterimageRelayUrl) {
    return new DownvoteEventKindTypePlugin(
        eventEntityService,
        reputationEventKindTypePlugin,
        afterimageInstanceIdentity,
        afterimageRelayUrl);
  }
}
