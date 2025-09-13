package com.prosilion.afterimage.config;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.relay.AfterimageReqService;
import com.prosilion.afterimage.service.AfterimageReputationCalculator;
import com.prosilion.afterimage.service.event.plugin.AfterimageFollowSetsEventPlugin;
import com.prosilion.afterimage.service.event.plugin.AfterimageRelaySetsEventPlugin;
import com.prosilion.afterimage.service.event.plugin.DownvoteEventPlugin;
import com.prosilion.afterimage.service.event.plugin.ReputationEventPlugin;
import com.prosilion.afterimage.service.event.plugin.SuperconductorFollowsListEventPlugin;
import com.prosilion.afterimage.service.event.plugin.UpvoteEventPlugin;
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
import com.prosilion.superconductor.base.service.event.service.EventKindService;
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
  EventKindTypePluginIF reputationEventPlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventPluginIF eventPlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull AfterimageReputationCalculator afterimageReputationCalculator) {
    return new ReputationEventPlugin(
        notifierService,
        new EventKindTypePlugin(
            AfterimageKindType.REPUTATION,
            eventPlugin),
        redisCacheServiceIF,
        aImgIdentity,
        afterimageReputationCalculator);
  }

  @Bean
  EventKindTypePluginIF upvoteEventKindTypePlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull EventKindPluginIF afterimageFollowSetsEventPlugin,
      @NonNull Identity aImgIdentity) {
    return new UpvoteEventPlugin(
        new EventKindTypePlugin(
            SuperconductorKindType.UPVOTE,
            eventPlugin),
        afterimageFollowSetsEventPlugin,
        aImgIdentity);
  }

  @Bean
  EventKindTypePluginIF downvoteEventKindTypePlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull EventKindPluginIF afterimageFollowSetsEventPlugin,
      @NonNull Identity aImgIdentity) {
    return new DownvoteEventPlugin(
        new EventKindTypePlugin(
            SuperconductorKindType.DOWNVOTE,
            eventPlugin),
        afterimageFollowSetsEventPlugin,
        aImgIdentity);
  }

  @Bean
  EventKindPluginIF superconductorFollowsListEventPlugin(
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull EventKindTypeServiceIF eventKindTypeService,
      @NonNull Identity aImgIdentity,
      @NonNull EventPluginIF eventPlugin) {
    return new SuperconductorFollowsListEventPlugin(
        new EventKindPlugin(
            Kind.SEARCH_RELAYS_LIST,
            eventPlugin),
        eventKindTypeService,
        redisCacheServiceIF,
        aImgIdentity);
  }

  @Bean
  EventKindPluginIF afterimageFollowSetsEventPlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventPluginIF eventPlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull EventKindTypePluginIF reputationEventPlugin) {
    return new AfterimageFollowSetsEventPlugin(
        notifierService,
        new EventKindPlugin(
            Kind.FOLLOW_SETS,
            eventPlugin),
        redisCacheServiceIF,
        aImgIdentity,
        reputationEventPlugin);
  }

  @Bean
  EventKindPluginIF afterimageRelaySetsEventPlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull List<EventKindPluginIF> eventKindPlugins,
      @NonNull Identity aImgIdentity) {
    return new AfterimageRelaySetsEventPlugin(
        new EventKindPlugin(
            Kind.RELAY_SETS,
            eventPlugin),
        new EventKindService(eventKindPlugins),
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
