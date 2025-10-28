package com.prosilion.afterimage.config;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.web.EventApiAuthUi;
import com.prosilion.afterimage.config.web.EventApiNoAuthUi;
import com.prosilion.afterimage.config.web.ReqApiAuthUi;
import com.prosilion.afterimage.config.web.ReqApiNoAuthUi;
import com.prosilion.afterimage.db.AfterimageCacheService;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.service.event.plugin.AfterimageFollowSetsEventPlugin;
import com.prosilion.afterimage.service.event.plugin.AfterimageRelaySetsEventPlugin;
import com.prosilion.afterimage.service.event.plugin.ReputationEventPlugin;
import com.prosilion.afterimage.service.event.plugin.SuperconductorSearchRelaysListEventPlugin;
import com.prosilion.afterimage.service.event.plugin.UniversalVoteEventPlugin;
import com.prosilion.afterimage.service.reputation.ReputationCalculationServiceIF;
import com.prosilion.afterimage.service.request.AfterimageReqService;
import com.prosilion.afterimage.service.request.ReqKindServiceIF;
import com.prosilion.afterimage.service.request.ReqKindTypeServiceIF;
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
import com.prosilion.superconductor.base.controller.EventApiUiIF;
import com.prosilion.superconductor.base.controller.ReqApiUiIF;
import com.prosilion.superconductor.base.service.event.auth.EventKindsAuthIF;
import com.prosilion.superconductor.base.service.event.auth.ReqAuthCondition;
import com.prosilion.superconductor.base.service.event.auth.ReqNoAuthCondition;
import com.prosilion.superconductor.base.service.event.service.EventKindService;
import com.prosilion.superconductor.base.service.event.service.EventKindTypeService;
import com.prosilion.superconductor.base.service.event.service.EventKindTypeServiceIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePlugin;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.EventKindPlugin;
import com.prosilion.superconductor.base.service.event.type.EventPluginIF;
import com.prosilion.superconductor.base.service.event.type.KindTypeIF;
import com.prosilion.superconductor.base.service.request.NotifierService;
import com.prosilion.superconductor.base.service.request.ReqServiceIF;
import com.prosilion.superconductor.lib.redis.service.DeletionEventNoSqlEntityService;
import com.prosilion.superconductor.lib.redis.service.EventNosqlEntityService;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

import static com.prosilion.afterimage.enums.AfterimageKindType.UNIT_REPUTATION;
import static com.prosilion.afterimage.enums.AfterimageKindType.UNIT_VOTE;

@Slf4j
public abstract class AfterimageBaseConfig {
  public static final String UNIT_UPVOTE = "UNIT_UPVOTE";
  public static final String UNIT_DOWNVOTE = "UNIT_DOWNVOTE";
  public static final String PLUS_ONE_FORMULA = "+1";
  public static final String MINUS_ONE_FORMULA = "-1";

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
  AfterimageCacheService afterimageCacheService(
      @NonNull EventNosqlEntityService eventNosqlEntityService,
      @NonNull DeletionEventNoSqlEntityService deletionEventNoSqlEntityService,
      @NonNull Identity afterimageInstanceIdentity) {
    return new AfterimageCacheService(eventNosqlEntityService, deletionEventNoSqlEntityService, afterimageInstanceIdentity);
  }


  @Bean
//      (name = "reputationEventPlugin")
  EventKindTypePluginIF reputationEventPlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull NotifierService notifierService,
      @NonNull AfterimageCacheService afterimageCacheService,
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF) {
    return new ReputationEventPlugin(
        notifierService,
        new EventKindTypePlugin(
            UNIT_REPUTATION,
            eventPlugin),
        afterimageCacheService,
        afterimageInstanceIdentity,
        reputationCalculationServiceIF);
  }

  @Bean
  EventKindTypeServiceIF eventKindTypeServiceIF(
      @NonNull List<EventKindTypePluginIF> eventKindTypePlugins,
      @NonNull EventPluginIF eventPlugin,
      @NonNull EventKindPluginIF afterimageFollowSetsEventPlugin,
      @NonNull Identity afterimageInstanceIdentity) {
    return new EventKindTypeService(
        eventKindTypePlugins,
        new UniversalVoteEventPlugin(
            new EventKindTypePlugin(
                UNIT_VOTE,
                eventPlugin),
            afterimageFollowSetsEventPlugin,
            afterimageInstanceIdentity));
  }

  @Bean
  EventKindPluginIF superconductorSearchRelaysListEventPlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull EventKindTypeServiceIF eventKindTypeServiceIF,
      @NonNull Identity afterimageInstanceIdentity) {
    return new SuperconductorSearchRelaysListEventPlugin(
        new EventKindPlugin(
            Kind.SEARCH_RELAYS_LIST,
            eventPlugin),
        eventKindTypeServiceIF,
        redisCacheServiceIF,
        afterimageInstanceIdentity);
  }

  @Bean
  EventKindPluginIF afterimageRelaySetsEventPlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull List<EventKindPluginIF> eventKindPlugins,
      @NonNull Identity afterimageInstanceIdentity) {
    return new AfterimageRelaySetsEventPlugin(
        new EventKindPlugin(
            Kind.RELAY_SETS,
            eventPlugin),
        new EventKindService(eventKindPlugins),
        redisCacheServiceIF,
        afterimageInstanceIdentity);
  }

  @Bean
//      (name = "afterimageFollowSetsEventPlugin")
  EventKindPluginIF afterimageFollowSetsEventPlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull EventKindTypePluginIF reputationEventPlugin,
      @NonNull NotifierService notifierService,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity afterimageInstanceIdentity) {
    return new AfterimageFollowSetsEventPlugin(
        notifierService,
        new EventKindPlugin(
            Kind.FOLLOW_SETS,
            eventPlugin),
        redisCacheServiceIF,
        afterimageInstanceIdentity,
        reputationEventPlugin);
  }

  @Bean
//      (name = "badgeDefinitionUpvoteEvent")
  BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent(@NonNull Identity afterimageInstanceIdentity) {
    return new BadgeDefinitionAwardEvent(afterimageInstanceIdentity, new IdentifierTag(UNIT_UPVOTE));
  }

  @Bean
//      (name = "badgeDefinitionDownvoteEvent")
  BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent(@NonNull Identity afterimageInstanceIdentity) {
    return new BadgeDefinitionAwardEvent(afterimageInstanceIdentity, new IdentifierTag(UNIT_DOWNVOTE));
  }

  @Bean
//      (name = "badgeDefinitionReputationEvent")
  BadgeDefinitionReputationEvent badgeDefinitionReputationEvent(
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent,
      @NonNull BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent) throws ParseException {
    return new BadgeDefinitionReputationEvent(
        afterimageInstanceIdentity,
        new IdentifierTag(
            UNIT_REPUTATION.getName()),
        List.of(
            new FormulaEvent(
                afterimageInstanceIdentity,
                badgeDefinitionUpvoteEvent,
                PLUS_ONE_FORMULA),
            new FormulaEvent(
                afterimageInstanceIdentity,
                badgeDefinitionDownvoteEvent,
                MINUS_ONE_FORMULA)));
  }

  @Bean
  DataLoaderRedisIF dataLoaderRedis(
      @NonNull AfterimageCacheService afterimageCacheService,
//      @NonNull @Qualifier("badgeDefinitionReputationEvent")
      BadgeDefinitionReputationEvent badgeDefinitionReputationEvent
//      ,
//      @NonNull @Qualifier("badgeDefinitionUpvoteEvent") BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent,
//      @NonNull @Qualifier("badgeDefinitionDownvoteEvent") BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent
  ) {
    return new DataLoaderRedis(
        afterimageCacheService,
        badgeDefinitionReputationEvent
//        ,
//        badgeDefinitionUpvoteEvent,
//        badgeDefinitionDownvoteEvent
    );
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
