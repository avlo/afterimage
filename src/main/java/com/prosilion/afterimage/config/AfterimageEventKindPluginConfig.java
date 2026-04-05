package com.prosilion.afterimage.config;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFormulaEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.award.CacheBadgeAwardGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.award.CacheBadgeAwardReputationEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.tag.CacheDereferenceAddressTagService;
import com.prosilion.superconductor.autoconfigure.base.service.event.tag.CacheDereferenceEventTagService;
import com.prosilion.superconductor.lib.redis.service.RedisCacheService;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class AfterimageEventKindPluginConfig {
  @Bean
  CacheDereferenceAddressTagService cacheDereferenceAddressTagService(
      @NonNull RedisCacheService redisCacheService,
      @NonNull String afterimageRelayUrl,
      @NonNull Duration requestTimeoutDuration) {
    CacheDereferenceAddressTagService cacheDereferenceAddressTagService =
        new CacheDereferenceAddressTagService(
            redisCacheService,
            afterimageRelayUrl,
            Duration.ofMinutes(30));
    return cacheDereferenceAddressTagService;
  }

  @Bean
  CacheDereferenceEventTagService cacheDereferenceEventTagService(
      @NonNull RedisCacheService redisCacheService,
      @NonNull String afterimageRelayUrl,
      @NonNull Duration requestTimeoutDuration) {
    CacheDereferenceEventTagService cacheDereferenceEventTagService =
        new CacheDereferenceEventTagService(
            redisCacheService,
            afterimageRelayUrl,
            Duration.ofMinutes(30));
    return cacheDereferenceEventTagService;
  }

  @Bean("eventKindMaterializers")
  Map<Kind, Function<EventIF, BaseEvent>> eventKindMaterializers(
      @NonNull CacheBadgeAwardGenericEventService cacheBadgeAwardGenericEventService,
      @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
      @NonNull CacheBadgeAwardReputationEventService cacheBadgeAwardReputationEventService,
      @NonNull CacheFormulaEventService cacheFormulaEventService) {
    Map<Kind, Function<EventIF, BaseEvent>> kindFxnMap = new HashMap<>();

    kindFxnMap.put(
        Kind.BADGE_AWARD_EVENT,
        cacheBadgeAwardGenericEventService::materialize);

    kindFxnMap.put(
        Kind.BADGE_DEFINITION_EVENT,
        cacheBadgeDefinitionGenericEventService::materialize);

    kindFxnMap.put(
        Kind.FOLLOW_SETS,
        cacheBadgeAwardReputationEventService::materialize);

    kindFxnMap.put(
        Kind.ARBITRARY_CUSTOM_APP_DATA,
        cacheFormulaEventService::materialize);

    return kindFxnMap;
  }

  @Bean("eventKindTypeMaterializers")
  Map<Kind, Function<EventIF, BaseEvent>> eventKindTypeMaterializers(
      @NonNull CacheBadgeAwardReputationEventService cacheBadgeAwardReputationEventService,
      @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService) {
    Map<Kind, Function<EventIF, BaseEvent>> kindFxnMap = new HashMap<>();

    kindFxnMap.put(
        Kind.BADGE_AWARD_EVENT,
        cacheBadgeAwardReputationEventService::materialize);

    kindFxnMap.put(
        Kind.BADGE_DEFINITION_EVENT,
        cacheBadgeDefinitionReputationEventService::materialize);

    return kindFxnMap;
  }
}
