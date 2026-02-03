package com.prosilion.afterimage.config;

import com.prosilion.afterimage.config.web.EventApiAuthUi;
import com.prosilion.afterimage.config.web.EventApiNoAuthUi;
import com.prosilion.afterimage.config.web.ReqApiAuthUi;
import com.prosilion.afterimage.config.web.ReqApiNoAuthUi;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.service.event.plugin.AfterimageFollowSetsEventPlugin;
import com.prosilion.afterimage.service.event.plugin.AfterimageRelaySetsEventPlugin;
import com.prosilion.afterimage.service.event.plugin.BadgeAwardReputationEventKindTypeRedisPlugin;
import com.prosilion.afterimage.service.event.plugin.BadgeDefinitionGenericEventKindRedisPlugin;
import com.prosilion.afterimage.service.event.plugin.BadgeDefinitionReputationEventKindTypeRedisPlugin;
import com.prosilion.afterimage.service.event.plugin.FormulaEventKindPlugin;
import com.prosilion.afterimage.service.event.plugin.SuperconductorSearchRelaysListEventPlugin;
import com.prosilion.afterimage.service.event.plugin.UniversalVoteEventPlugin;
import com.prosilion.afterimage.service.reputation.ReputationCalculationServiceIF;
import com.prosilion.afterimage.service.request.AfterimageReqService;
import com.prosilion.afterimage.service.request.ReqKindServiceIF;
import com.prosilion.afterimage.service.request.ReqKindTypeServiceIF;
import com.prosilion.nostr.codec.deserializer.EventMessageDeserializer;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.EventKindsAuth;
import com.prosilion.superconductor.autoconfigure.base.EventKindsAuthCondition;
import com.prosilion.superconductor.autoconfigure.base.EventKindsNoAuthCondition;
import com.prosilion.superconductor.autoconfigure.base.service.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.base.controller.EventApiUiIF;
import com.prosilion.superconductor.base.controller.ReqApiUiIF;
import com.prosilion.superconductor.base.service.CacheBadgeAwardGenericEventServiceIF;
import com.prosilion.superconductor.base.service.CacheBadgeAwardReputationEventServiceIF;
import com.prosilion.superconductor.base.service.CacheBadgeDefinitionGenericEventServiceIF;
import com.prosilion.superconductor.base.service.CacheBadgeDefinitionReputationEventServiceIF;
import com.prosilion.superconductor.base.service.CacheDereferenceAddressTagServiceIF;
import com.prosilion.superconductor.base.service.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.service.CacheFormulaEventServiceIF;
import com.prosilion.superconductor.base.service.CacheTagMappedEventServiceIF;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.EventService;
import com.prosilion.superconductor.base.service.event.auth.EventKindsAuthIF;
import com.prosilion.superconductor.base.service.event.auth.ReqAuthCondition;
import com.prosilion.superconductor.base.service.event.auth.ReqNoAuthCondition;
import com.prosilion.superconductor.base.service.event.service.EventKindService;
import com.prosilion.superconductor.base.service.event.service.EventKindServiceIF;
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

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_KIND_TYPE;
import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_DEFINITION_REPUTATION_KIND_TYPE;

@Slf4j
public abstract class AfterimageBaseConfig {
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

//  @Bean
//  AfterimageCacheService afterimageCacheService(
//      CacheEventTagBaseEventServiceIF cacheBadgeDefinitionReputationEventService,
//      @Qualifier("cacheBadgeAwardEventService") CacheEventTagBaseEventServiceIF cacheBadgeAwardEventService) {
//    return new AfterimageCacheService(
//        (CacheBadgeDefinitionReputationEventService) cacheBadgeDefinitionReputationEventService,
//        cacheBadgeAwardEventService);
//  }

//  @Bean("badgeAwardGenericEventKindPlugin")
//  EventKindPluginIF badgeAwardGenericEventKindPlugin(
//      @NonNull NotifierService notifierService,
//      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin) {
//    BadgeAwardGenericEventKindRedisPlugin badgeAwardGenericEventKindRedisPlugin = new BadgeAwardGenericEventKindRedisPlugin(
//        notifierService,
//        new EventKindPlugin(
//            Kind.BADGE_AWARD_EVENT,
//            eventPlugin));
//    return badgeAwardGenericEventKindRedisPlugin;
//  }

  @Bean("badgeAwardReputationEventKindTypePlugin")
  EventKindTypePluginIF badgeAwardReputationEventKindTypePlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull NotifierService notifierService,
      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF,
      @NonNull @Qualifier("cacheBadgeAwardReputationEventService") CacheTagMappedEventServiceIF cacheBadgeAwardReputationEventService,
      @NonNull @Qualifier("cacheBadgeDefinitionReputationEventService") CacheTagMappedEventServiceIF cacheBadgeDefinitionReputationEventServiceIF,
      @NonNull @Qualifier("cacheDereferenceAddressTagService") CacheDereferenceAddressTagServiceIF cacheDereferenceAddressTagService,
      @NonNull @Qualifier("cacheFormulaEventService") CacheFormulaEventServiceIF cacheFormulaEventServiceIF) {
    return new BadgeAwardReputationEventKindTypeRedisPlugin(
        afterimageRelayUrl,
        afterimageInstanceIdentity,
        notifierService,
        new EventKindTypePlugin(
            BADGE_AWARD_REPUTATION_KIND_TYPE,
            eventPlugin),
        cacheServiceIF,
        reputationCalculationServiceIF,
        (CacheBadgeAwardReputationEventServiceIF) cacheBadgeAwardReputationEventService,
        (CacheBadgeDefinitionReputationEventService) cacheBadgeDefinitionReputationEventServiceIF,
        cacheFormulaEventServiceIF);
  }

  @Bean
  EventKindTypePluginIF badgeDefinitionReputationEventKindTypePlugin(
      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
      @NonNull @Qualifier("cacheBadgeDefinitionReputationEventService") CacheTagMappedEventServiceIF cacheBadgeDefinitionReputationEventService) {
    return new BadgeDefinitionReputationEventKindTypeRedisPlugin(
        new EventKindTypePlugin(
            BADGE_DEFINITION_REPUTATION_KIND_TYPE,
            eventPlugin),
        (CacheBadgeDefinitionReputationEventServiceIF) cacheBadgeDefinitionReputationEventService);
  }

  @Bean
  EventKindPluginIF badgeDefinitionGenericEventKindPlugin(@NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin) {
    BadgeDefinitionGenericEventKindRedisPlugin badgeDefinitionGenericEventKindRedisPlugin = new BadgeDefinitionGenericEventKindRedisPlugin(
        new EventKindPlugin(
            Kind.BADGE_DEFINITION_EVENT,
            eventPlugin));
    return badgeDefinitionGenericEventKindRedisPlugin;
  }

  @Bean
  EventKindPluginIF formulaEventKindPlugin(
      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
      @NonNull @Qualifier("cacheFormulaEventService") CacheTagMappedEventServiceIF cacheFormulaEventService) {
    EventKindPlugin eventKindPlugin = new EventKindPlugin(
        Kind.ARBITRARY_CUSTOM_APP_DATA,
        eventPlugin);
    FormulaEventKindPlugin formulaEventKindPlugin = new FormulaEventKindPlugin(
        eventKindPlugin,
//        TODO: finish below rxr
        (CacheFormulaEventServiceIF) cacheFormulaEventService);
    return formulaEventKindPlugin;
  }

  @Bean
//  @Primary
  EventKindPluginIF universalVoteEventPlugin(
      EventPluginIF eventPlugin,
      AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin,
      Identity afterimageInstanceIdentity,
      String afterimageRelayUrl,
      CacheServiceIF cacheServiceIF,
      @NonNull @Qualifier("cacheFormulaEventService") CacheFormulaEventServiceIF cacheFormulaEventServiceIF,
      @NonNull @Qualifier("cacheBadgeAwardGenericEventService") CacheBadgeAwardGenericEventServiceIF cacheBadgeAwardGenericEventServiceIF,
      @NonNull @Qualifier("cacheBadgeDefinitionGenericEventService") CacheBadgeDefinitionGenericEventServiceIF cacheBadgeDefinitionGenericEventServiceIF,
      @NonNull @Qualifier("cacheBadgeDefinitionReputationEventService") CacheBadgeDefinitionReputationEventServiceIF cacheBadgeDefinitionReputationEventServiceIF,
      @NonNull @Qualifier("cacheFollowSetsEventService") CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF) {
    UniversalVoteEventPlugin universalVoteEventPlugin = new UniversalVoteEventPlugin(
        afterimageRelayUrl,
        cacheServiceIF,
        cacheBadgeAwardGenericEventServiceIF,
        cacheBadgeDefinitionGenericEventServiceIF,
        cacheFormulaEventServiceIF,
        cacheBadgeDefinitionReputationEventServiceIF,
        cacheFollowSetsEventServiceIF,
        afterimageFollowSetsEventPlugin,
        new EventKindPlugin(
            Kind.BADGE_AWARD_EVENT,
            eventPlugin),
        afterimageInstanceIdentity);
    return universalVoteEventPlugin;
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

//  @Bean(name = "cacheDereferenceEventTagServiceClient")
//  @Primary
//  CacheDereferenceEventTagServiceIF cacheDereferenceEventTagService(
//      @NonNull @Qualifier("cacheDereferenceEventTagService") CacheDereferenceEventTagServiceIF cacheDereferenceEventTagServiceIF) {
//    return new CacheDereferenceEventTagServiceClient(cacheDereferenceEventTagServiceIF);
//  }
//
//  @Bean(name = "cacheDereferenceAddressTagServiceClient")
//  @Primary
//  CacheDereferenceAddressTagServiceIF cacheDereferenceAddressTagService(
//      @NonNull @Qualifier("cacheDereferenceAddressTagService") CacheDereferenceAddressTagServiceIF cacheDereferenceAddressTagServiceIF) {
//    return new CacheDereferenceAddressTagServiceClient(cacheDereferenceAddressTagServiceIF);
//  }

  @Bean
//      (name = "afterimageFollowSetsEventPlugin")
  AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin(
      String afterimageRelayUrl,
      EventPluginIF eventPlugin,
      @Qualifier("badgeAwardReputationEventKindTypePlugin") EventKindTypePluginIF badgeAwardReputationEventKindTypePlugin,
      NotifierService notifierService,
      CacheServiceIF cacheService,
      @NonNull @Qualifier("cacheFollowSetsEventService") CacheTagMappedEventServiceIF cacheFollowSetsEventServiceIF,
      @NonNull @Qualifier("cacheBadgeAwardGenericEventService") CacheTagMappedEventServiceIF cacheBadgeAwardGenericEventServiceIF,
      Identity afterimageInstanceIdentity) {
    return new AfterimageFollowSetsEventPlugin(
        afterimageRelayUrl,
        notifierService,
        new EventKindPlugin(
            Kind.FOLLOW_SETS,
            eventPlugin),
        cacheService,
        (CacheFollowSetsEventServiceIF) cacheFollowSetsEventServiceIF,
        (CacheBadgeAwardGenericEventServiceIF) cacheBadgeAwardGenericEventServiceIF,
        afterimageInstanceIdentity,
        badgeAwardReputationEventKindTypePlugin);
  }

//  @Bean
//  @Primary
//  EventPlugin eventPlugin(
//      List<CacheEventTagBaseEventServiceIF> cacheEventTagBaseEventServiceIFS,
//      CacheServiceIF cacheService) {
//    return new EventPlugin(cacheEventTagBaseEventServiceIFS, cacheService);
//  }

//  @Bean(name = "badgeDefinitionUpvoteEvent")
//  BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent(
//      Identity afterimageInstanceIdentity,
//      String afterimageRelayUrl) {
//    return new BadgeDefinitionAwardEvent(
//        afterimageInstanceIdentity,
//        new IdentifierTag(UNIT_UPVOTE),
//        new Relay(afterimageRelayUrl));
//  }
//
//  @Bean(name = "badgeDefinitionDownvoteEvent")
//  BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent(
//      Identity afterimageInstanceIdentity,
//      String afterimageRelayUrl) {
//    return new BadgeDefinitionAwardEvent(
//        afterimageInstanceIdentity,
//        new IdentifierTag(UNIT_DOWNVOTE),
//        new Relay(afterimageRelayUrl));
//  }

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
