package com.prosilion.afterimage.config;

import com.prosilion.afterimage.config.web.EventApiAuthUi;
import com.prosilion.afterimage.config.web.EventApiNoAuthUi;
import com.prosilion.afterimage.config.web.ReqApiAuthUi;
import com.prosilion.afterimage.config.web.ReqApiNoAuthUi;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.service.event.plugin.AfterimageBadgeAwardReputationEventKindTypePlugin;
import com.prosilion.afterimage.service.event.plugin.AfterimageFollowSetsEventKindPlugin;
import com.prosilion.afterimage.service.event.plugin.AfterimageRelaySetsEventKindPlugin;
import com.prosilion.afterimage.service.event.plugin.SuperconductorSearchRelaysListEventKindPlugin;
import com.prosilion.afterimage.service.event.plugin.UniversalVoteEventKindPlugin;
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
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.base.cache.CacheBadgeAwardReputationEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheCurationSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheFormulaEventServiceIF;
import com.prosilion.superconductor.base.cache.tag.CacheKindAddressTagServiceIF;
import com.prosilion.superconductor.base.controller.EventApiUiIF;
import com.prosilion.superconductor.base.controller.ReqApiUiIF;
import com.prosilion.superconductor.base.service.event.auth.EventKindsAuthIF;
import com.prosilion.superconductor.base.service.event.auth.ReqAuthCondition;
import com.prosilion.superconductor.base.service.event.auth.ReqNoAuthCondition;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.BadgeDefinitionReputationEventKindTypePlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.EventKindTypePlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import com.prosilion.superconductor.base.service.request.ReqServiceIF;
import com.prosilion.superconductor.base.service.request.subscriber.NotifierService;
import com.prosilion.superconductor.lib.redis.service.RedisCacheService;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;

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
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull CacheBadgeAwardReputationEventServiceIF cacheBadgeAwardReputationEventServiceIF) {
    AfterimageBadgeAwardReputationEventKindTypePlugin afterimageBadgeAwardReputationEventKindTypePlugin = new AfterimageBadgeAwardReputationEventKindTypePlugin(
       afterimageRelayUrl,
       aImgIdentity,
       notifierService,
       new EventKindTypePlugin(
          BADGE_AWARD_REPUTATION_KIND_TYPE,
          eventPlugin),
       redisCacheService,
       reputationCalculationServiceIF,
       cacheFollowSetsEventService,
       cacheBadgeAwardReputationEventServiceIF);
    return afterimageBadgeAwardReputationEventKindTypePlugin;
  }

  @Bean("followSetsEventKindPlugin")
  AfterimageFollowSetsEventKindPlugin followSetsEventKindPlugin(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull String afterimageRelayUrl,
     @NonNull EventPlugin eventPlugin,
     @NonNull NotifierService notifierService,
     @NonNull RedisCacheService redisCacheService,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull CacheKindAddressTagServiceIF cacheKindAddressTagServiceIF,
     @NonNull AfterimageBadgeAwardReputationEventKindTypePlugin badgeAwardReputationEventKindTypePlugin) {
    return new AfterimageFollowSetsEventKindPlugin(
       afterimageRelayUrl,
       notifierService,
       eventPlugin,
       redisCacheService,
       cacheFollowSetsEventService,
       cacheKindAddressTagServiceIF,
       afterimageInstanceIdentity,
       badgeAwardReputationEventKindTypePlugin);
  }

  @Bean("badgeAwardGenericEventKindPlugin")
  UniversalVoteEventKindPlugin badgeAwardGenericEventKindPlugin(
     @NonNull String afterimageRelayUrl,
     @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
     @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull AfterimageFollowSetsEventKindPlugin followSetsEventKindPlugin,
     @NonNull CacheFormulaEventServiceIF cacheFormulaEventServiceIF,
     @NonNull CacheCurationSetsEventServiceIF cacheCurationSetsEventServiceIF,
     @NonNull EventPlugin eventPlugin,
     @NonNull Identity afterimageInstanceIdentity) {
    return new UniversalVoteEventKindPlugin(
       afterimageRelayUrl,
       cacheBadgeDefinitionGenericEventService,
       cacheBadgeDefinitionReputationEventService,
       cacheFollowSetsEventService,
       followSetsEventKindPlugin,
       cacheFormulaEventServiceIF,
       cacheCurationSetsEventServiceIF,
       eventPlugin,
       afterimageInstanceIdentity);
  }

  @Bean
  SuperconductorSearchRelaysListEventKindPlugin superconductorSearchRelaysListEventKindPlugin(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull RedisCacheService redisCacheService,
     @NonNull EventPlugin eventPlugin,
     @NonNull UniversalVoteEventKindPlugin badgeAwardGenericEventKindPlugin) {
    return new SuperconductorSearchRelaysListEventKindPlugin(
       afterimageInstanceIdentity,
       redisCacheService,
       eventPlugin,
       badgeAwardGenericEventKindPlugin);
  }

  @Bean
  AfterimageRelaySetsEventKindPlugin afterimageRelaySetsEventKindPlugin(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull RedisCacheService redisCacheService,
     @NonNull EventPlugin eventPlugin,
     @NonNull AfterimageFollowSetsEventKindPlugin followSetsEventKindPlugin) {
    return new AfterimageRelaySetsEventKindPlugin(
       afterimageInstanceIdentity,
       redisCacheService,
       eventPlugin,
       followSetsEventKindPlugin);
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
