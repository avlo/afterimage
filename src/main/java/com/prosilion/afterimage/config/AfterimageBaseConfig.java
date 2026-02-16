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
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
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
import com.prosilion.superconductor.base.cache.CacheBadgeAwardGenericEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.controller.EventApiUiIF;
import com.prosilion.superconductor.base.controller.ReqApiUiIF;
import com.prosilion.superconductor.base.service.event.EventService;
import com.prosilion.superconductor.base.service.event.auth.EventKindsAuthIF;
import com.prosilion.superconductor.base.service.event.auth.ReqAuthCondition;
import com.prosilion.superconductor.base.service.event.auth.ReqNoAuthCondition;
import com.prosilion.superconductor.base.service.event.kind.EventKindService;
import com.prosilion.superconductor.base.service.event.kind.EventKindServiceIF;
import com.prosilion.superconductor.base.service.event.kind.type.EventKindTypeServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPluginIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.MaterializedEventKindPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.ParameterizedEventKindPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.EventKindTypePlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import com.prosilion.superconductor.base.service.request.ReqServiceIF;
import com.prosilion.superconductor.base.service.request.subscriber.NotifierService;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
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
    log.info("Loading custom AfterImage kind types\n{}", values);
    return values;
  }

  @Bean
  public EventMessageDeserializer eventMessageDeserializer() {
    EventMessageDeserializer eventMessageDeserializer = new EventMessageDeserializer();
    log.info("EventMessageDeserializer instance [{}]", eventMessageDeserializer);
    return eventMessageDeserializer;
  }

  @Bean("kindClassStringMap")
  public Map<String, String> kindClassStringMap() {
    ResourceBundle relaysBundle = ResourceBundle.getBundle("kind-class-map");
    Map<String, String> collect = relaysBundle.keySet().stream()
        .collect(Collectors.toMap(key -> key, relaysBundle::getString));
    return collect;
  }

  @Bean
  ParameterizedEventKindPlugin defaultEventKindPlugin(
      @NonNull NotifierService notifierService,
      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
      @NonNull CacheBadgeAwardGenericEventService cacheBadgeAwardGenericEventService,
      @NonNull @Qualifier("kindClassStringMap") Map<String, String> kindClassStringMap) {
    ParameterizedEventKindPlugin parameterizedEventKindPlugin = new ParameterizedEventKindPlugin(
        notifierService,
        new MaterializedEventKindPlugin(
            Kind.TEXT_NOTE, eventPlugin, cacheBadgeAwardGenericEventService),
        kindClassStringMap);
    log.debug("loaded custom defaultEventKindPlugin bean {}", parameterizedEventKindPlugin.getClass().getSimpleName());
    log.debug("with kindClassStringMap contents:\n{}", prettyPrintKindClassStringMap(kindClassStringMap));
    return parameterizedEventKindPlugin;
  }

  private String prettyPrintKindClassStringMap(Map<String, String> kindClassStringMap) {
    final String formatter = "     %-5s   :   %s";
    return kindClassStringMap.entrySet().stream().map(entry ->
            String.format(formatter, entry.getKey(), entry.getValue()))
        .sorted()
        .collect(Collectors.joining("\n"));
  }

  @Bean("badgeAwardReputationEventKindTypePlugin")
  EventKindTypePluginIF badgeAwardReputationEventKindTypePlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull NotifierService notifierService,
      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF,
      @NonNull CacheBadgeAwardReputationEventService cacheBadgeAwardReputationEventService,
      @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventServiceIF,
      @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventServiceIF,
      @NonNull CacheFormulaEventService cacheFormulaEventServiceIF,
      @NonNull CacheFollowSetsEventService cacheFollowSetsEventServiceIF) {
    return new BadgeAwardReputationEventKindTypeRedisPlugin(
        afterimageRelayUrl,
        afterimageInstanceIdentity,
        notifierService,
        new EventKindTypePlugin(
            BADGE_AWARD_REPUTATION_KIND_TYPE,
            eventPlugin,
            cacheBadgeDefinitionReputationEventServiceIF),
        cacheServiceIF,
        reputationCalculationServiceIF,
        cacheBadgeDefinitionGenericEventServiceIF,
        cacheBadgeAwardReputationEventService,
        cacheBadgeDefinitionReputationEventServiceIF,
        cacheFormulaEventServiceIF,
        cacheFollowSetsEventServiceIF);
  }

  @Bean("badgeDefinitionReputationEventKindTypePlugin")
  EventKindTypePluginIF badgeDefinitionReputationEventKindTypePlugin(
      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
      @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService) {
    return new BadgeDefinitionReputationEventKindTypeRedisPlugin(
        new EventKindTypePlugin(
            BADGE_DEFINITION_REPUTATION_KIND_TYPE,
            eventPlugin,
            cacheBadgeDefinitionReputationEventService), cacheBadgeDefinitionReputationEventService);
  }

  @Bean("badgeDefinitionGenericEventKindPlugin")
  EventKindPluginIF badgeDefinitionGenericEventKindPlugin(
      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
      @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService) {
    BadgeDefinitionGenericEventKindRedisPlugin badgeDefinitionGenericEventKindRedisPlugin =
        new BadgeDefinitionGenericEventKindRedisPlugin(
            new MaterializedEventKindPlugin(
                Kind.BADGE_DEFINITION_EVENT,
                eventPlugin,
                cacheBadgeDefinitionGenericEventService),
            cacheBadgeDefinitionGenericEventService);
    return badgeDefinitionGenericEventKindRedisPlugin;
  }

  @Bean("formulaEventKindPlugin")
  FormulaEventKindPlugin formulaEventKindPlugin(
      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
      @NonNull CacheFormulaEventService cacheFormulaEventService) {
    MaterializedEventKindPlugin eventKindPlugin = new MaterializedEventKindPlugin(
        Kind.ARBITRARY_CUSTOM_APP_DATA,
        eventPlugin,
        cacheFormulaEventService);
    FormulaEventKindPlugin formulaEventKindPlugin = new FormulaEventKindPlugin(
        eventKindPlugin,
        cacheFormulaEventService);
    return formulaEventKindPlugin;
  }

  @Bean
  EventService eventService(
      @Qualifier("eventKindService") EventKindServiceIF eventKindService,
      @Qualifier("eventKindTypeService") EventKindTypeServiceIF eventKindTypeService) {
    EventService eventService = new EventService(eventKindService, eventKindTypeService);
    return eventService;
  }

  @Bean("superconductorSearchRelaysListEventPlugin")
  SuperconductorSearchRelaysListEventPlugin superconductorSearchRelaysListEventPlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull CacheServiceIF cacheService,
      @NonNull UniversalVoteEventPlugin universalVoteEventPlugin,
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull CacheBadgeAwardGenericEventService cacheBadgeAwardGenericEventService) {
    return new SuperconductorSearchRelaysListEventPlugin(
        new MaterializedEventKindPlugin(
            Kind.SEARCH_RELAYS_LIST,
            eventPlugin,
            cacheBadgeAwardGenericEventService),
        universalVoteEventPlugin,
        cacheService,
        afterimageInstanceIdentity);
  }

  @Bean("afterimageRelaySetsEventPlugin")
  AfterimageRelaySetsEventPlugin afterimageRelaySetsEventPlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull CacheServiceIF cacheService,
      @NonNull List<EventKindPluginIF> eventKindPlugins,
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull CacheBadgeAwardGenericEventService cacheBadgeAwardGenericEventService) {
    return new AfterimageRelaySetsEventPlugin(
        new MaterializedEventKindPlugin(
            Kind.RELAY_SETS,
            eventPlugin,
            cacheBadgeAwardGenericEventService),
        new EventKindService(eventKindPlugins),
        cacheService,
        afterimageInstanceIdentity);
  }

  @Bean("afterimageFollowSetsEventPlugin")
  AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin(
      String afterimageRelayUrl,
      EventPluginIF eventPlugin,
      @Qualifier("badgeAwardReputationEventKindTypePlugin") EventKindTypePluginIF badgeAwardReputationEventKindTypePlugin,
      NotifierService notifierService,
      CacheServiceIF cacheService,
      @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
      @NonNull CacheBadgeAwardGenericEventService cacheBadgeAwardGenericEventServiceIF,
      Identity afterimageInstanceIdentity) {
    return new AfterimageFollowSetsEventPlugin(
        afterimageRelayUrl,
        notifierService,
        new MaterializedEventKindPlugin(
            Kind.FOLLOW_SETS,
            eventPlugin,
            cacheFollowSetsEventServiceIF),
        cacheService,
        cacheFollowSetsEventServiceIF,
        cacheBadgeAwardGenericEventServiceIF,
        afterimageInstanceIdentity,
        badgeAwardReputationEventKindTypePlugin);
  }

  @Bean("universalVoteEventPlugin")
  UniversalVoteEventPlugin universalVoteEventPlugin(
      EventPluginIF eventPlugin,
      AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin,
      Identity afterimageInstanceIdentity,
      String afterimageRelayUrl,
      CacheServiceIF cacheServiceIF,
      @NonNull CacheFormulaEventService cacheFormulaEventServiceIF,
      @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventServiceIF,
      @NonNull CacheBadgeAwardGenericEventServiceIF<BadgeDefinitionGenericEvent, BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> cacheBadgeAwardGenericEventServiceIF,
      @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventServiceIF,
      @NonNull CacheFollowSetsEventService cacheFollowSetsEventServiceIF) {
    UniversalVoteEventPlugin universalVoteEventPlugin = new UniversalVoteEventPlugin(
        afterimageRelayUrl,
        cacheServiceIF,
        cacheBadgeDefinitionGenericEventServiceIF,
        cacheFormulaEventServiceIF,
        cacheBadgeAwardGenericEventServiceIF,
        cacheBadgeDefinitionReputationEventServiceIF,
        cacheFollowSetsEventServiceIF,
        afterimageFollowSetsEventPlugin,
        new MaterializedEventKindPlugin(
            Kind.BADGE_AWARD_EVENT,
            eventPlugin,
            cacheBadgeAwardGenericEventServiceIF),
        afterimageInstanceIdentity);
    return universalVoteEventPlugin;
  }

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
