package com.prosilion.afterimage.config;

import com.prosilion.afterimage.config.web.EventApiAuthUi;
import com.prosilion.afterimage.config.web.EventApiNoAuthUi;
import com.prosilion.afterimage.config.web.ReqApiAuthUi;
import com.prosilion.afterimage.config.web.ReqApiNoAuthUi;
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
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.EventKindsAuth;
import com.prosilion.superconductor.autoconfigure.base.EventKindsAuthCondition;
import com.prosilion.superconductor.autoconfigure.base.EventKindsNoAuthCondition;
import com.prosilion.superconductor.base.controller.EventApiUiIF;
import com.prosilion.superconductor.base.controller.ReqApiUiIF;
import com.prosilion.superconductor.base.service.CacheBadgeAwardGenericVoteEventServiceIF;
import com.prosilion.superconductor.base.service.CacheBadgeAwardReputationEventServiceIF;
import com.prosilion.superconductor.base.service.CacheBadgeDefinitionReputationEventServiceIF;
import com.prosilion.superconductor.base.service.CacheDereferenceAddressTagServiceIF;
import com.prosilion.superconductor.base.service.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.EventService;
import com.prosilion.superconductor.base.service.event.auth.EventKindsAuthIF;
import com.prosilion.superconductor.base.service.event.auth.ReqAuthCondition;
import com.prosilion.superconductor.base.service.event.auth.ReqNoAuthCondition;
import com.prosilion.superconductor.base.service.event.service.EventKindService;
import com.prosilion.superconductor.base.service.event.service.EventKindServiceIF;
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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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

  public final static String PLATFORM = BadgeDefinitionReputationEvent.class.getPackageName();
  public final static String IDENTITY = BadgeDefinitionReputationEvent.class.getSimpleName();
  public final static String PROOF = String.valueOf(BadgeDefinitionReputationEvent.class.hashCode());

  @Bean
  Identity afterimageInstanceIdentity(@Value("${afterimage.key.private}") String privateKey) {
    Identity identity = Identity.create(privateKey);
    return identity;
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
    log.info("Loading custom AfterImage kind types [{}]", values);
    return values;
  }

  @Bean
  public EventMessageDeserializer eventMessageDeserializer() {
    EventMessageDeserializer eventMessageDeserializer = new EventMessageDeserializer();
    log.info("EventMessageDeserializer instance [{}]", eventMessageDeserializer);
    return eventMessageDeserializer;
  }

//  @Bean(name = "cacheBadgeAwardEventService")
//  CacheBadgeAwardReputationEventService cacheBadgeAwardEventService(
//      CacheServiceIF cacheService) {
//    return new CacheBadgeAwardReputationEventService(cacheService);
//  }

//  @Bean
//  AfterimageCacheService afterimageCacheService(
//      CacheEventTagBaseEventServiceIF cacheBadgeDefinitionReputationEventService,
//      @Qualifier("cacheBadgeAwardEventService") CacheEventTagBaseEventServiceIF cacheBadgeAwardEventService) {
//    return new AfterimageCacheService(
//        (CacheBadgeDefinitionReputationEventService) cacheBadgeDefinitionReputationEventService,
//        cacheBadgeAwardEventService);
//  }

  @Bean
//      (name = "reputationEventPlugin")
  ReputationEventPlugin reputationEventPlugin(
      EventPluginIF eventPlugin,
      NotifierService notifierService,
//      @NonNull @Qualifier("redisCacheService") CacheServiceIF cacheServiceIF,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF,
      @NonNull CacheDereferenceAddressTagServiceIF cacheDereferenceAddressTagServiceIF,
      @NonNull CacheBadgeDefinitionReputationEventServiceIF cacheBadgeDefinitionReputationEventServiceIF,
      @NonNull CacheBadgeAwardReputationEventServiceIF cacheBadgeAwardReputationEventServiceIF,
      Identity afterimageInstanceIdentity) {
    return new ReputationEventPlugin(
        notifierService,
        new EventKindTypePlugin(
            UNIT_REPUTATION,
            eventPlugin),
        afterimageInstanceIdentity,
        cacheServiceIF,
        reputationCalculationServiceIF,
        cacheDereferenceAddressTagServiceIF,
        cacheBadgeDefinitionReputationEventServiceIF,
        cacheBadgeAwardReputationEventServiceIF
    );
  }

  @Bean
  @Primary
  EventKindTypeService eventKindTypeService(
      List<EventKindTypePluginIF> eventKindTypePlugins,
      EventPluginIF eventPlugin,
      EventKindPluginIF afterimageFollowSetsEventPlugin,
      Identity afterimageInstanceIdentity) {
    EventKindTypeService eventKindTypeService = new EventKindTypeService(
        eventKindTypePlugins,
        new UniversalVoteEventPlugin(
            new EventKindTypePlugin(
                UNIT_VOTE,
                eventPlugin),
            afterimageFollowSetsEventPlugin
//            , afterimageInstanceIdentity
        ));
    return eventKindTypeService;
  }

  @Bean
  EventService eventService(
      @Qualifier("eventKindService") EventKindServiceIF eventKindService,
      @Qualifier("eventKindTypeService") EventKindTypeServiceIF eventKindTypeService) {
    EventService eventService = new EventService(eventKindService, eventKindTypeService);
    return eventService;
  }

  @Bean
  SuperconductorSearchRelaysListEventPlugin superconductorSearchRelaysListEventPlugin(
      EventPluginIF eventPlugin,
      CacheServiceIF cacheService,
      EventKindTypeServiceIF eventKindTypeService,
      Identity afterimageInstanceIdentity) {
    return new SuperconductorSearchRelaysListEventPlugin(
        new EventKindPlugin(
            Kind.SEARCH_RELAYS_LIST,
            eventPlugin),
        eventKindTypeService,
        cacheService,
        afterimageInstanceIdentity);
  }

  @Bean
  AfterimageRelaySetsEventPlugin afterimageRelaySetsEventPlugin(
      EventPluginIF eventPlugin,
      CacheServiceIF cacheService,
      List<EventKindPluginIF> eventKindPlugins,
      Identity afterimageInstanceIdentity) {
    return new AfterimageRelaySetsEventPlugin(
        new EventKindPlugin(
            Kind.RELAY_SETS,
            eventPlugin),
        new EventKindService(eventKindPlugins),
        cacheService,
        afterimageInstanceIdentity);
  }

  @Bean
//      (name = "afterimageFollowSetsEventPlugin")
  AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin(
      String afterimageRelayUrl,
      EventPluginIF eventPlugin,
      EventKindTypePluginIF reputationEventPlugin,
      NotifierService notifierService,
      CacheServiceIF cacheService,
      @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
      @NonNull CacheBadgeAwardGenericVoteEventServiceIF cacheBadgeAwardGenericVoteEventServiceIF,
      Identity afterimageInstanceIdentity) {
    return new AfterimageFollowSetsEventPlugin(
        afterimageRelayUrl,
        notifierService,
        new EventKindPlugin(
            Kind.FOLLOW_SETS,
            eventPlugin),
        cacheService,
        cacheFollowSetsEventServiceIF,
        cacheBadgeAwardGenericVoteEventServiceIF,
        afterimageInstanceIdentity,
        reputationEventPlugin);
  }

  @Bean
  ExternalIdentityTag externalIdentityTag() {
    return new ExternalIdentityTag(PLATFORM, IDENTITY, PROOF);
  }

//  @Bean
//  @Primary
//  EventPlugin eventPlugin(
//      List<CacheEventTagBaseEventServiceIF> cacheEventTagBaseEventServiceIFS,
//      CacheServiceIF cacheService) {
//    return new EventPlugin(cacheEventTagBaseEventServiceIFS, cacheService);
//  }

  @Bean(name = "badgeDefinitionUpvoteEvent")
  BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent(
      Identity afterimageInstanceIdentity,
      String afterimageRelayUrl) {
    return new BadgeDefinitionAwardEvent(
        afterimageInstanceIdentity,
        new IdentifierTag(UNIT_UPVOTE),
        new Relay(afterimageRelayUrl));
  }

  @Bean(name = "badgeDefinitionDownvoteEvent")
  BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent(
      Identity afterimageInstanceIdentity,
      String afterimageRelayUrl) {
    return new BadgeDefinitionAwardEvent(
        afterimageInstanceIdentity,
        new IdentifierTag(UNIT_DOWNVOTE),
        new Relay(afterimageRelayUrl));
  }

//  @Bean(name = "formulaEventPlusOne")
//  FormulaEvent formulaEventPlusOne(
//      String afterimageRelayUrl,
//      Identity afterimageInstanceIdentity,
//      @Qualifier("badgeDefinitionUpvoteEvent") BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent) throws ParseException {
//    return new FormulaEvent(
//        afterimageInstanceIdentity,
//        badgeDefinitionUpvoteEvent,
//        PLUS_ONE_FORMULA);
//  }
//
//  @Bean(name = "formulaEventMinusOne")
//  FormulaEvent formulaEventMinusOne(
//      Identity afterimageInstanceIdentity,
//      @Qualifier("badgeDefinitionDownvoteEvent") BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent) throws ParseException {
//    return new FormulaEvent(
//        afterimageInstanceIdentity,
//        badgeDefinitionDownvoteEvent,
//        MINUS_ONE_FORMULA);
//  }

//  @Bean
//  BadgeDefinitionReputationEvent badgeDefinitionReputationEvent(
//      String afterimageRelayUrl,
//      Identity afterimageInstanceIdentity,
//      ExternalIdentityTag externalIdentityTag,
//      @Qualifier("formulaEventPlusOne") FormulaEvent formulaEventPlusOne,
//      @Qualifier("formulaEventMinusOne") FormulaEvent formulaEventMinusOne) {
//    return new BadgeDefinitionReputationEvent(
//        afterimageInstanceIdentity,
//        new IdentifierTag(
//            UNIT_REPUTATION.getName()),
//        new Relay(afterimageRelayUrl),
//        externalIdentityTag,
//        List.of(
//            formulaEventPlusOne,
//            formulaEventMinusOne));
//  }

//  @Bean
//  DataLoaderRedisIF dataLoaderRedis(
//      AfterimageCacheService afterimageCacheService,
//      @Qualifier("badgeDefinitionUpvoteEvent") BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent,
//      @Qualifier("badgeDefinitionDownvoteEvent") BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent,
//      BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) {
//    return new DataLoaderRedis(
//        afterimageCacheService,
//        badgeDefinitionUpvoteEvent,
//        badgeDefinitionDownvoteEvent,
//        badgeDefinitionReputationEvent);
//  }

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
