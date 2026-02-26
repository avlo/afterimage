package com.prosilion.afterimage.config;

import com.prosilion.afterimage.config.web.EventApiAuthUi;
import com.prosilion.afterimage.config.web.EventApiNoAuthUi;
import com.prosilion.afterimage.config.web.ReqApiAuthUi;
import com.prosilion.afterimage.config.web.ReqApiNoAuthUi;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.service.event.plugin.AfterimageFollowSetsEventPlugin;
import com.prosilion.afterimage.service.event.plugin.AfterimageRelaySetsEventPlugin;
import com.prosilion.afterimage.service.event.plugin.AfterimageBadgeAwardReputationEventKindTypePlugin;
import com.prosilion.afterimage.service.event.plugin.SuperconductorSearchRelaysListEventPlugin;
import com.prosilion.afterimage.service.event.plugin.UniversalVoteEventPlugin;
import com.prosilion.afterimage.service.reputation.ReputationCalculationServiceIF;
import com.prosilion.afterimage.service.request.AfterimageReqService;
import com.prosilion.afterimage.service.request.ReqKindServiceIF;
import com.prosilion.afterimage.service.request.ReqKindTypeServiceIF;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.EventKindsAuth;
import com.prosilion.superconductor.autoconfigure.base.EventKindsAuthCondition;
import com.prosilion.superconductor.autoconfigure.base.EventKindsNoAuthCondition;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFormulaEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.award.CacheBadgeAwardGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.award.CacheBadgeAwardReputationEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.base.controller.EventApiUiIF;
import com.prosilion.superconductor.base.controller.ReqApiUiIF;
import com.prosilion.superconductor.base.service.event.auth.EventKindsAuthIF;
import com.prosilion.superconductor.base.service.event.auth.ReqAuthCondition;
import com.prosilion.superconductor.base.service.event.auth.ReqNoAuthCondition;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.BadgeAwardReputationEventKindTypePlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.BadgeDefinitionReputationEventKindTypePlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.EventKindTypePlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import com.prosilion.superconductor.base.service.request.ReqServiceIF;
import com.prosilion.superconductor.base.service.request.subscriber.NotifierService;
import com.prosilion.superconductor.lib.redis.service.RedisCacheService;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

import static com.prosilion.superconductor.base.service.event.plugin.kind.type.SuperconductorKindType.BADGE_AWARD_REPUTATION_KIND_TYPE;
import static com.prosilion.superconductor.base.service.event.plugin.kind.type.SuperconductorKindType.BADGE_DEFINITION_REPUTATION_KIND_TYPE;

@Slf4j
public abstract class AfterimageBaseConfig {
  @Bean
  Identity afterimageInstanceIdentity(@Value("${afterimage.key.private}") String privateKey) {
    return Identity.create(privateKey);
  }

  @Bean
  @Primary
  AfterimageReqService afterimageReqService(
      ReqServiceIF reqService,
      ReqKindServiceIF reqKindService,
      ReqKindTypeServiceIF reqKindTypeService) {
    return new AfterimageReqService(reqService, reqKindService, reqKindTypeService);
  }

  @Bean
  List<KindTypeIF> kindTypes() {
    List<KindTypeIF> values = List.of(AfterimageKindType.values());
    log.info("Loading custom AfterImage kind types\n{}", values);
    return values;
  }

  @Bean("kindClassStringMap")
  public Map<String, String> kindClassStringMap() {
    ResourceBundle relaysBundle = ResourceBundle.getBundle("kind-class-map");
    Map<String, String> collect = relaysBundle.keySet().stream()
        .collect(Collectors.toMap(key -> key, relaysBundle::getString));
    return collect;
  }

  @Bean("badgeDefinitionReputationEventKindTypePlugin")
  BadgeDefinitionReputationEventKindTypePlugin badgeDefinitionReputationEventKindTypePlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull EventPlugin eventPlugin) {
    return new BadgeDefinitionReputationEventKindTypePlugin(
        afterimageRelayUrl,
        new EventKindTypePlugin(
            BADGE_DEFINITION_REPUTATION_KIND_TYPE,
            eventPlugin));
  }

  @Bean("badgeAwardReputationEventKindTypePlugin")
  AfterimageBadgeAwardReputationEventKindTypePlugin badgeAwardReputationEventKindTypePlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull Identity aImgIdentity,
      @NonNull EventPlugin eventPlugin,
      @NonNull NotifierService notifierService,
      @NonNull RedisCacheService redisCacheService,
      @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF,
      @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
      @NonNull CacheBadgeAwardReputationEventService cacheBadgeAwardReputationEventService,
      @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
      @NonNull CacheFormulaEventService cacheFormulaEventService,
      @NonNull CacheFollowSetsEventService cacheFollowSetsEventService) {
    return new AfterimageBadgeAwardReputationEventKindTypePlugin(
        afterimageRelayUrl,
        aImgIdentity,
        notifierService,
        new EventKindTypePlugin(
            BADGE_AWARD_REPUTATION_KIND_TYPE,
            eventPlugin),
        redisCacheService,
        reputationCalculationServiceIF,
        cacheBadgeDefinitionGenericEventService,
        cacheBadgeAwardReputationEventService,
        cacheBadgeDefinitionReputationEventService,
        cacheFormulaEventService,
        cacheFollowSetsEventService);
  }

  @Bean("followSetsEventKindPlugin")
  AfterimageFollowSetsEventPlugin followSetsEventKindPlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull EventPlugin eventPlugin,
      @NonNull BadgeAwardReputationEventKindTypePlugin badgeAwardReputationEventKindTypePlugin,
      @NonNull NotifierService notifierService,
      @NonNull RedisCacheService redisCacheService,
      @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
      @NonNull CacheBadgeAwardGenericEventService cacheBadgeAwardGenericEventService,
      Identity afterimageInstanceIdentity) {
    return new AfterimageFollowSetsEventPlugin(
        afterimageRelayUrl,
        notifierService,
        eventPlugin,
        redisCacheService,
        cacheFollowSetsEventService,
        cacheBadgeAwardGenericEventService,
        afterimageInstanceIdentity,
        badgeAwardReputationEventKindTypePlugin);
  }

  @Bean
  AfterimageRelaySetsEventPlugin afterimageRelaySetsEventPlugin(
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull RedisCacheService redisCacheService,
      @NonNull EventPlugin eventPlugin,
      @NonNull AfterimageFollowSetsEventPlugin followSetsEventKindPlugin,
      @NonNull CacheBadgeAwardReputationEventService cacheBadgeDefinitionReputationEventService) {
    return new AfterimageRelaySetsEventPlugin(
        afterimageInstanceIdentity,
        redisCacheService,
        eventPlugin,
        followSetsEventKindPlugin,
        cacheBadgeDefinitionReputationEventService::materialize);
  }

  @Bean("badgeAwardGenericEventKindPlugin")
  UniversalVoteEventPlugin badgeAwardGenericEventKindPlugin(
      @NonNull EventPlugin eventPlugin,
      @NonNull AfterimageFollowSetsEventPlugin followSetsEventKindPlugin,
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull String afterimageRelayUrl,
      @NonNull RedisCacheService redisCacheService,
      @NonNull CacheFormulaEventService cacheFormulaEventService,
      @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
      @NonNull CacheBadgeAwardGenericEventService cacheBadgeAwardGenericEventService,
      @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
      @NonNull CacheFollowSetsEventService cacheFollowSetsEventService) {
    UniversalVoteEventPlugin universalVoteEventPlugin = new UniversalVoteEventPlugin(
        afterimageRelayUrl,
        redisCacheService,
        cacheBadgeDefinitionGenericEventService,
        cacheFormulaEventService,
        cacheBadgeAwardGenericEventService,
        cacheBadgeDefinitionReputationEventService,
        cacheFollowSetsEventService,
        followSetsEventKindPlugin,
        eventPlugin,
        afterimageInstanceIdentity);
    return universalVoteEventPlugin;
  }

  @Bean
  SuperconductorSearchRelaysListEventPlugin superconductorSearchRelaysListEventPlugin(
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull RedisCacheService redisCacheService,
      @NonNull EventPlugin eventPlugin,
      @NonNull UniversalVoteEventPlugin badgeAwardGenericEventKindPlugin,
      @NonNull CacheBadgeAwardGenericEventService cacheBadgeAwardGenericEventService) {
    return new SuperconductorSearchRelaysListEventPlugin(
        afterimageInstanceIdentity,
        redisCacheService,
        eventPlugin,
        badgeAwardGenericEventKindPlugin,
        cacheBadgeAwardGenericEventService::materialize);
  }

  @Bean
  @Conditional(EventKindsAuthCondition.class)
  EventKindsAuthIF EventKindsAuth(@Value("#{'${superconductor.auth.event.kinds}'.split(',')}") List<String> authEventKinds) {
    return new EventKindsAuth(authEventKinds.stream().map(Kind::valueOf).toList());
  }

  @Bean
  @Conditional(EventKindsAuthCondition.class)
  EventApiUiIF eventApiAuthUiIF() {
    return new EventApiAuthUi();
  }

  @Bean
  @Conditional(EventKindsNoAuthCondition.class)
  EventApiUiIF eventApiNoAuthUiIF() {
    return new EventApiNoAuthUi();
  }

  @Bean
  @Conditional(ReqAuthCondition.class)
  ReqApiUiIF reqApiAuthUiIF() {
    return new ReqApiAuthUi();
  }

  @Bean
  @Conditional(ReqNoAuthCondition.class)
  ReqApiUiIF reqApiNoAuthUiIF() {
    return new ReqApiNoAuthUi();
  }
}
