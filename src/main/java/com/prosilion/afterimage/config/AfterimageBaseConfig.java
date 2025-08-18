package com.prosilion.afterimage.config;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.relay.AfterimageReqService;
import com.prosilion.afterimage.service.event.plugin.DownvoteEventKindTypePlugin;
import com.prosilion.afterimage.service.event.plugin.ReputationPublishingEventKindTypePlugin;
import com.prosilion.afterimage.service.event.plugin.SuperconductorFollowsListNonPublishingEventKindPlugin;
import com.prosilion.afterimage.service.event.plugin.UpvoteEventKindTypePlugin;
import com.prosilion.afterimage.service.request.ReqKindServiceIF;
import com.prosilion.afterimage.service.request.ReqKindTypeServiceIF;
import com.prosilion.nostr.codec.deserializer.EventMessageDeserializer;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.ReferenceTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.redis.config.DataLoaderRedisIF;
import com.prosilion.superconductor.base.service.event.service.EventKindTypeServiceIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePlugin;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.EventKindPlugin;
import com.prosilion.superconductor.base.service.event.type.EventPluginIF;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import com.prosilion.superconductor.base.service.request.NotifierService;
import com.prosilion.superconductor.base.service.request.ReqServiceIF;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

@Slf4j
public abstract class AfterimageBaseConfig {
  @Bean
  Identity afterimageInstanceIdentity(@NonNull @Value("${afterimage.key.private}") String privateKey) {
    return Identity.create(privateKey);
  }

  @Bean
  @Primary
  ReqServiceIF afterimageReqService(
      @NonNull ReqServiceIF reqService,
      @NonNull ReqKindServiceIF reqKindService,
      @NonNull ReqKindTypeServiceIF reqKindTypeService) {
    return new AfterimageReqService(reqService, reqKindService, reqKindTypeService);
  }

  @Bean
  List<KindTypeIF> kindTypes() {
    List<KindTypeIF> values = List.of(AfterimageKindType.values());
    log.info("Loading custom AfterImage kind types [{}]", values);
    return values;
  }

  @Bean
  public EventMessageDeserializer eventMessageDeserializer() {
    EventMessageDeserializer eventMessageDeserializer = new EventMessageDeserializer();
    log.info("EventMessageDeserializer instance [{}]", eventMessageDeserializer);
    return eventMessageDeserializer;
  }

  @Bean
  EventKindTypePluginIF reputationEventKindTypePlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventPluginIF eventPlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    return new ReputationPublishingEventKindTypePlugin(
        notifierService,
        new EventKindTypePlugin(
            AfterimageKindType.REPUTATION,
            eventPlugin),
        redisCacheServiceIF,
        aImgIdentity,
        reputationBadgeDefinitionEvent);
  }

  @Bean
  EventKindTypePluginIF upvoteEventKindTypePlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull EventKindTypePluginIF reputationEventKindTypePlugin) {
    return new UpvoteEventKindTypePlugin(
        new EventKindTypePlugin(
            SuperconductorKindType.UPVOTE,
            eventPlugin),
        reputationEventKindTypePlugin);
  }

  @Bean
  EventKindTypePluginIF downvoteEventKindTypePlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull EventKindTypePluginIF reputationEventKindTypePlugin) {
    return new DownvoteEventKindTypePlugin(
        new EventKindTypePlugin(
            SuperconductorKindType.DOWNVOTE,
            eventPlugin),
        reputationEventKindTypePlugin);
  }

  @Bean
  EventKindPluginIF superconductorFollowsListNonPublishingEventKindPlugin(
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull EventKindTypeServiceIF eventKindTypeService,
      @NonNull Identity aImgIdentity,
      @NonNull EventPluginIF eventPlugin) {
    return new SuperconductorFollowsListNonPublishingEventKindPlugin(
        new EventKindPlugin(
            Kind.RELAY_LIST_METADATA,
            eventPlugin),
        eventKindTypeService,
        redisCacheServiceIF,
        aImgIdentity);
  }

  @Bean
  BadgeDefinitionEvent reputationBadgeDefinitionEvent(
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull String afterimageRelayUrl) throws NoSuchAlgorithmException {

    return new BadgeDefinitionEvent(
        afterimageInstanceIdentity,
        new IdentifierTag(
            AfterimageKindType.REPUTATION.getName()),
        new ReferenceTag(
            afterimageRelayUrl),
        "afterimage reputation definition f(x)");
  }

  @Bean
  DataLoaderRedisIF dataLoaderRedis(
      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    return new DataLoaderRedis(eventPlugin, reputationBadgeDefinitionEvent);
  }
}
