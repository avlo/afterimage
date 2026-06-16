package com.prosilion.afterimage.config;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFormulaEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.award.CacheBadgeAwardGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.award.CacheBadgeAwardReputationEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.tag.CacheReferenceAddressTagService;
import com.prosilion.superconductor.autoconfigure.base.service.event.tag.CacheReferenceEventTagService;
import com.prosilion.superconductor.autoconfigure.base.service.event.tag.RemoteAbstractTagService;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.kind.EventKindService;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.StandardEventKindPlugin;
import com.prosilion.superconductor.lib.redis.service.RedisCacheService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class AfterimageEventKindPluginConfig {
  @Bean
  CacheReferenceAddressTagService cacheReferenceAddressTagService(
     @NonNull RedisCacheService redisCacheService,
     @NonNull RemoteAbstractTagService remoteAbstractTagService) {
    return new CacheReferenceAddressTagService(redisCacheService, remoteAbstractTagService);
  }

  @Bean
  CacheReferenceEventTagService cacheReferenceEventTagService(
     @NonNull RedisCacheService redisCacheService,
     @NonNull RemoteAbstractTagService remoteAbstractTagService) {
    return new CacheReferenceEventTagService(redisCacheService, remoteAbstractTagService);
  }

  @Bean("kindClassStringMap")
  public Map<Kind, String> kindClassStringMap() {
    ResourceBundle relaysBundle = ResourceBundle.getBundle("kind-class-map");
    return relaysBundle.keySet().stream()
       .collect(Collectors.toMap(Kind::valueOf, relaysBundle::getString));
  }

  @Bean("standardEventKindPlugins")
  List<StandardEventKindPlugin> standardEventKindPlugins() {
    return List.of();
  }

  @Bean(name = "eventKindService")
  EventKindService eventKindService(
     @NonNull List<EventKindPluginIF> eventKindPlugins,
     @NonNull @Qualifier("standardEventKindPlugins") List<StandardEventKindPlugin> standardEventKindPlugins) {
    return new EventKindService(
       Stream.concat(
          eventKindPlugins.stream(),
          standardEventKindPlugins.stream()).toList());
  }

  @Bean
  EventPlugin eventPlugin(
     @NonNull CacheServiceIF cacheServiceIF,
     @NonNull @Qualifier("eventKindMaterializers") Map<Kind, Function<EventIF, BaseEvent>> eventKindMaterializers,
     @NonNull @Qualifier("eventKindTypeMaterializers") Map<Kind, Function<EventIF, BaseEvent>> eventKindTypeMaterializers,
     @NonNull @Qualifier("kindClassStringMap") Map<Kind, String> kindClassStringMap) {
    return new EventPlugin(
       cacheServiceIF,
       eventKindMaterializers,
       eventKindTypeMaterializers,
       kindClassStringMap);
  }

  @Bean("eventKindMaterializers")
  Map<Kind, Function<EventIF, BaseEvent>> eventKindMaterializers(
     @NonNull CacheBadgeAwardGenericEventService cacheBadgeAwardGenericEventService,
     @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
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
       cacheFollowSetsEventService::materialize);

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
