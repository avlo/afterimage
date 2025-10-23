package com.prosilion.afterimage.config;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.web.EventApiAuthUi;
import com.prosilion.afterimage.config.web.EventApiNoAuthUi;
import com.prosilion.afterimage.config.web.ReqApiAuthUi;
import com.prosilion.afterimage.config.web.ReqApiNoAuthUi;
import com.prosilion.afterimage.service.event.plugin.AfterimageFollowSetsEventPlugin;
import com.prosilion.afterimage.service.event.plugin.AfterimageRelaySetsEventPlugin;
import com.prosilion.afterimage.service.event.plugin.ReputationEventPlugin;
import com.prosilion.afterimage.service.event.plugin.SuperconductorSearchRelaysListEventPlugin;
import com.prosilion.afterimage.service.event.plugin.UniversalVoteEventPlugin;
import com.prosilion.afterimage.service.reputation.ReputationCalculationServiceIF;
import com.prosilion.afterimage.service.request.AfterimageReqService;
import com.prosilion.afterimage.service.request.ReqKindServiceIF;
import com.prosilion.nostr.codec.deserializer.EventMessageDeserializer;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.EventKindsAuth;
import com.prosilion.superconductor.autoconfigure.base.EventKindsAuthCondition;
import com.prosilion.superconductor.autoconfigure.base.EventKindsNoAuthCondition;
import com.prosilion.superconductor.autoconfigure.redis.config.DataLoaderRedisIF;
import com.prosilion.superconductor.base.controller.EventApiUiIF;
import com.prosilion.superconductor.base.controller.ReqApiUiIF;
import com.prosilion.superconductor.base.service.event.auth.EventKindsAuthIF;
import com.prosilion.superconductor.base.service.event.auth.ReqAuthCondition;
import com.prosilion.superconductor.base.service.event.auth.ReqNoAuthCondition;
import com.prosilion.superconductor.base.service.event.service.EventKindService;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.type.EventKindPlugin;
import com.prosilion.superconductor.base.service.event.type.EventPluginIF;
import com.prosilion.superconductor.base.service.request.NotifierService;
import com.prosilion.superconductor.base.service.request.ReqServiceIF;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

@Slf4j
public abstract class AfterimageBaseConfig {
  public static final String UNIT_REPUTATION = "UNIT_REPUTATION";
  public static final String UNIT_UPVOTE = "UNIT_UPVOTE";
  public static final String UNIT_DOWNVOTE = "UNIT_DOWNVOTE";
  public static final String PLUS_ONE_FORMULA = "+1";
  public static final String MINUS_ONE_FORMULA = "-1";

  @Bean
  Identity afterimageInstanceIdentity(@NonNull @Value("${afterimage.key.private}") String privateKey) {
    return Identity.create(privateKey);
  }

  @Bean
  public EventMessageDeserializer eventMessageDeserializer() {
    EventMessageDeserializer eventMessageDeserializer = new EventMessageDeserializer();
    log.info("EventMessageDeserializer instance [{}]", eventMessageDeserializer);
    return eventMessageDeserializer;
  }

  @Bean
  @Primary
  ReqServiceIF afterimageReqService(
      @NonNull ReqServiceIF reqService,
      @NonNull ReqKindServiceIF reqKindService) {
    return new AfterimageReqService(reqService, reqKindService);
  }

  @Bean
  EventKindPluginIF reputationEventPlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull NotifierService notifierService,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF,
      @NonNull Identity aImgIdentity) {
    return new ReputationEventPlugin(
        notifierService,
        new EventKindPlugin(
            Kind.BADGE_AWARD_EVENT,
            eventPlugin),
        redisCacheServiceIF,
        aImgIdentity,
        reputationCalculationServiceIF);
  }

  @Bean
  EventKindPluginIF superconductorSearchRelaysListEventPlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
//      @NonNull EventKindServiceIF eventKindServiceIF,
      @NonNull Identity aImgIdentity) {
    return new SuperconductorSearchRelaysListEventPlugin(
        new EventKindPlugin(
            Kind.SEARCH_RELAYS_LIST,
            eventPlugin),
//        eventKindServiceIF,
        redisCacheServiceIF,
        aImgIdentity);
  }

  @Bean
  EventKindPluginIF afterimageRelaySetsEventPlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity,
//      TODO: investigae List<>, seems superfluous since no longer KindTypeIF
      @NonNull List<EventKindPluginIF> eventKindPlugins) {
    return new AfterimageRelaySetsEventPlugin(
        new EventKindPlugin(
            Kind.RELAY_SETS,
            eventPlugin),
        new EventKindService(eventKindPlugins),
        redisCacheServiceIF,
        aImgIdentity);
  }

  @Bean
  EventKindPluginIF afterimageFollowSetsEventPlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull EventKindPluginIF reputationEventPlugin,
      @NonNull NotifierService notifierService,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity) {
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
  EventKindPluginIF generalVoteEventPlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull EventKindPluginIF afterimageFollowSetsEventPlugin,
      @NonNull Identity aImgIdentity) {
    return new UniversalVoteEventPlugin(
        new EventKindPlugin(
            Kind.BADGE_AWARD_EVENT,
            eventPlugin),
        afterimageFollowSetsEventPlugin,
        aImgIdentity);
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

  @Bean
  BadgeDefinitionReputationEvent badgeDefinitionReputationEvent(@NonNull Identity afterimageInstanceIdentity) throws ParseException {
    BadgeDefinitionReputationEvent badgeDefinitionReputationEvent = new BadgeDefinitionReputationEvent(
        afterimageInstanceIdentity,
        new IdentifierTag(
            UNIT_REPUTATION),
        List.of(
            new FormulaEvent(
                afterimageInstanceIdentity,
                new IdentifierTag(AfterimageBaseConfig.UNIT_UPVOTE),
                PLUS_ONE_FORMULA),
            new FormulaEvent(
                afterimageInstanceIdentity,
                new IdentifierTag(AfterimageBaseConfig.UNIT_DOWNVOTE),
                MINUS_ONE_FORMULA)));
    return badgeDefinitionReputationEvent;
  }

  @Bean
  DataLoaderRedisIF dataLoaderRedis(
      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
      @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent,
      @NonNull BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent, // Superconductor AutoConfigured bean
      @NonNull BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent) {  // Superconductor AutoConfigured bean
    return new DataLoaderRedis(
        eventPlugin,
        badgeDefinitionReputationEvent,
        badgeDefinitionUpvoteEvent,
        badgeDefinitionDownvoteEvent);
  }
}
