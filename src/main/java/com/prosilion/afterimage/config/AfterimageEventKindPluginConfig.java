package com.prosilion.afterimage.config;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFormulaEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.award.CacheBadgeAwardGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.award.CacheBadgeAwardReputationEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class AfterimageEventKindPluginConfig {

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

    kindFxnMap.put(
        Kind.DELETION,
        eventIF -> new DeletionEvent(
            eventIF.asGenericEventRecord()));

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
