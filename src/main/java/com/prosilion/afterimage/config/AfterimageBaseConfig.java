package com.prosilion.afterimage.config;

import com.prosilion.afterimage.config.web.EventApiAuthUi;
import com.prosilion.afterimage.config.web.EventApiNoAuthUi;
import com.prosilion.afterimage.config.web.ReqApiAuthUi;
import com.prosilion.afterimage.config.web.ReqApiNoAuthUi;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.service.event.plugin.AfterimageRelaySetsEventKindPlugin;
import com.prosilion.afterimage.service.event.plugin.SuperconductorSearchRelaysListEventKindPlugin;
import com.prosilion.afterimage.service.event.plugin.sc.BadgeAwardReputationEventKindTypePlugin;
import com.prosilion.afterimage.service.event.plugin.sc.BadgeSetsEventKindPlugin;
import com.prosilion.afterimage.service.event.plugin.sc.FollowSetsEventKindPlugin;
import com.prosilion.afterimage.service.event.plugin.sc.UniversalVoteEventKindPlugin;
import com.prosilion.afterimage.service.request.AfterimageReqService;
import com.prosilion.afterimage.service.request.ReqKindServiceIF;
import com.prosilion.afterimage.service.request.ReqKindTypeServiceIF;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.EventKindsAuth;
import com.prosilion.superconductor.autoconfigure.base.EventKindsAuthCondition;
import com.prosilion.superconductor.autoconfigure.base.EventKindsNoAuthCondition;
import com.prosilion.superconductor.autoconfigure.curation.plugin.kind.CuratedBadgeAwardCanonicalEventKindPlugin;
import com.prosilion.superconductor.autoconfigure.curation.service.CacheBadgeDefinitionReputationEventServiceIF;
import com.prosilion.superconductor.autoconfigure.curation.service.CacheBadgeSetsEventServiceIF;
import com.prosilion.superconductor.autoconfigure.curation.service.ReputationCalculationServiceIF;
import com.prosilion.superconductor.autoconfigure.curation.service.event.award.CacheCuratedBadgeAwardCanonicalEventService;
import com.prosilion.superconductor.autoconfigure.curation.service.event.definition.CacheCuratedBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.curation.service.event.sets.CacheBadgeSetsEventService;
import com.prosilion.superconductor.autoconfigure.curation.service.event.sets.CacheFollowSetsEventService;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.controller.EventApiUiIF;
import com.prosilion.superconductor.base.controller.ReqApiUiIF;
import com.prosilion.superconductor.base.service.event.DeleteEventServiceIF;
import com.prosilion.superconductor.base.service.event.auth.EventKindsAuthIF;
import com.prosilion.superconductor.base.service.event.auth.ReqAuthCondition;
import com.prosilion.superconductor.base.service.event.auth.ReqNoAuthCondition;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.EventKindTypePlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import static com.prosilion.superconductor.base.service.event.plugin.kind.type.SuperconductorKindType.BADGE_AWARD_REPUTATION_KIND_TYPE;
import com.prosilion.superconductor.base.service.request.ReqServiceIF;
import com.prosilion.superconductor.base.service.request.subscriber.NotifierService;
import com.prosilion.superconductor.lib.redis.service.RedisCacheService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;

import java.util.List;

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

  @Bean("badgeAwardReputationEventKindTypePlugin")
  BadgeAwardReputationEventKindTypePlugin badgeAwardReputationEventKindTypePlugin(
     @NonNull Identity superconductorInstanceIdentity,
     @NonNull String superconductorRelayUrl,
     @NonNull EventPlugin eventPlugin,
     @NonNull NotifierService notifierService,
     @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull DeleteEventServiceIF deleteEventServiceIF,
     @NonNull CacheServiceIF cacheServiceIF) {
    return new BadgeAwardReputationEventKindTypePlugin(
       superconductorRelayUrl,
       superconductorInstanceIdentity,
       notifierService,
       new EventKindTypePlugin(
          BADGE_AWARD_REPUTATION_KIND_TYPE,
          eventPlugin),
       reputationCalculationServiceIF,
       cacheFollowSetsEventService,
       deleteEventServiceIF,
       cacheServiceIF);
  }

  @Bean("badgeSetsEventKindPlugin")
  BadgeSetsEventKindPlugin badgeSetsEventKindPlugin(
     @NonNull Identity superconductorInstanceIdentity,
     @NonNull String superconductorRelayUrl,
     @NonNull CacheBadgeSetsEventService cacheBadgeSetsEventService,
     @NonNull CacheCuratedBadgeAwardCanonicalEventService cacheCuratedBadgeAwardCanonicalEventService,
     @NonNull DeleteEventServiceIF deleteEventServiceIF,
     @NonNull EventPlugin eventPlugin,
     @NonNull CacheServiceIF cacheServiceIF) {
    return new BadgeSetsEventKindPlugin(
       superconductorInstanceIdentity,
       superconductorRelayUrl,
       cacheBadgeSetsEventService,
       cacheCuratedBadgeAwardCanonicalEventService,
       deleteEventServiceIF,
       eventPlugin,
       cacheServiceIF);
  }

  @Bean("followSetsEventKindPlugin")
  FollowSetsEventKindPlugin followSetsEventKindPlugin(
     @NonNull Identity superconductorInstanceIdentity,
     @NonNull String superconductorRelayUrl,
     @NonNull EventPlugin eventPlugin,
     @NonNull NotifierService notifierService,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull CacheBadgeSetsEventServiceIF cacheBadgeSetsEventServiceIF,
     @NonNull CacheBadgeDefinitionReputationEventServiceIF cacheBadgeDefinitionReputationEventServiceIF,
     @NonNull BadgeSetsEventKindPlugin badgeSetsEventKindPlugin,
     @NonNull BadgeAwardReputationEventKindTypePlugin badgeAwardReputationEventKindTypePlugin,
     @NonNull CacheServiceIF cacheServiceIF) {
    return new FollowSetsEventKindPlugin(
       superconductorInstanceIdentity,
       superconductorRelayUrl,
       notifierService,
       eventPlugin,
       cacheFollowSetsEventService,
       cacheBadgeSetsEventServiceIF,
       cacheBadgeDefinitionReputationEventServiceIF,
       badgeSetsEventKindPlugin,
       badgeAwardReputationEventKindTypePlugin,
       cacheServiceIF);
  }

  @Bean("badgeAwardCanonicalEventKindPlugin")
  UniversalVoteEventKindPlugin badgeAwardCanonicalEventKindPlugin(
     @NonNull String superconductorRelayUrl,
     @NonNull CacheCuratedBadgeDefinitionGenericEventService cacheCuratedBadgeDefinitionGenericEventService,
     @NonNull @Qualifier("followSetsEventKindPlugin") FollowSetsEventKindPlugin followSetsEventKindPlugin,
     @NonNull CuratedBadgeAwardCanonicalEventKindPlugin curatedBadgeAwardCanonicalEventKindPlugin,
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull CacheCuratedBadgeAwardCanonicalEventService cacheCuratedBadgeAwardCanonicalEventService,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull CacheServiceIF cacheServiceIF) {
    return new UniversalVoteEventKindPlugin(
       superconductorRelayUrl,
       cacheCuratedBadgeDefinitionGenericEventService,
       followSetsEventKindPlugin,
       curatedBadgeAwardCanonicalEventKindPlugin,
       afterimageInstanceIdentity,
       cacheCuratedBadgeAwardCanonicalEventService,
       cacheFollowSetsEventService,
       cacheServiceIF);
  }

  @Bean
  SuperconductorSearchRelaysListEventKindPlugin superconductorSearchRelaysListEventKindPlugin(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull RedisCacheService redisCacheService,
     @NonNull EventPlugin eventPlugin,
     @NonNull UniversalVoteEventKindPlugin universalVoteEventKindPlugin) {
    return new SuperconductorSearchRelaysListEventKindPlugin(
       afterimageInstanceIdentity,
       redisCacheService,
       eventPlugin,
       universalVoteEventKindPlugin);
  }

  @Bean
  AfterimageRelaySetsEventKindPlugin afterimageRelaySetsEventKindPlugin(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull RedisCacheService redisCacheService,
     @NonNull EventPlugin eventPlugin,
     @NonNull FollowSetsEventKindPlugin followSetsEventKindPlugin) {
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
