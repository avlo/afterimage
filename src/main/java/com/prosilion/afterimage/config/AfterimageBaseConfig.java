package com.prosilion.afterimage.config;

import com.prosilion.afterimage.config.web.EventApiAuthUi;
import com.prosilion.afterimage.config.web.EventApiNoAuthUi;
import com.prosilion.afterimage.config.web.ReqApiAuthUi;
import com.prosilion.afterimage.config.web.ReqApiNoAuthUi;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.service.event.plugin.AfterimageRelaySetsEventKindPlugin;
import com.prosilion.afterimage.service.event.plugin.SuperconductorSearchRelaysListEventKindPlugin;
import com.prosilion.afterimage.service.request.AfterimageReqService;
import com.prosilion.afterimage.service.request.ReqKindServiceIF;
import com.prosilion.afterimage.service.request.ReqKindTypeServiceIF;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.EventKindsAuth;
import com.prosilion.superconductor.autoconfigure.base.EventKindsAuthCondition;
import com.prosilion.superconductor.autoconfigure.base.EventKindsNoAuthCondition;
import com.prosilion.superconductor.autoconfigure.curation.plugin.kind.FollowSetsEventKindPlugin;
import com.prosilion.superconductor.autoconfigure.curation.plugin.kind.UniversalVoteEventKindPlugin;
import com.prosilion.superconductor.base.controller.EventApiUiIF;
import com.prosilion.superconductor.base.controller.ReqApiUiIF;
import com.prosilion.superconductor.base.service.event.auth.EventKindsAuthIF;
import com.prosilion.superconductor.base.service.event.auth.ReqAuthCondition;
import com.prosilion.superconductor.base.service.event.auth.ReqNoAuthCondition;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import com.prosilion.superconductor.base.service.request.ReqServiceIF;
import com.prosilion.superconductor.lib.redis.service.RedisCacheService;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;

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
