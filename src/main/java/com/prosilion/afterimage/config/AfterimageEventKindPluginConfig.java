package com.prosilion.afterimage.config;

import com.prosilion.afterimage.service.request.ReqKindService;
import com.prosilion.afterimage.service.request.plugin.ReqKindPlugin;
import com.prosilion.afterimage.service.request.plugin.ReqKindPluginIF;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.superconductor.base.service.event.plugin.kind.StandardEventKindPlugin;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AfterimageEventKindPluginConfig {

  @Bean
  List<ReqKindPluginIF> reqKindPluginList(@Value("${afterimage.req.kinds:}") String reqKinds) {
    List<String> stringStream = Arrays.stream(reqKinds.split(",")).map(String::trim).toList();
    log.debug("strings- afterimage.req.kinds:\n  {}", String.join(",\n  ", stringStream));

    List<Kind> kindStream = stringStream.stream().map(Kind::valueOf).toList();
    log.debug("kinds- afterimage.req.kinds:\n  {}", kindStream.stream()
       .map(Kind::getValue)
       .map(String::valueOf)
       .collect(Collectors.joining(",\n  ")));

    List<ReqKindPluginIF> reqKindPluginIFList = (reqKinds.isBlank()
       ? Arrays.stream(Kind.values())
       : kindStream.stream()
    ).map(ReqKindPlugin::new).collect(Collectors.toUnmodifiableList());

    log.debug("List<ReqKindPluginIF>:\n  {}", reqKindPluginIFList.stream().map(ReqKindPluginIF::getKind)
       .map(Kind::getValue)
       .map(String::valueOf)
       .collect(Collectors.joining(",\n  ")));
    return reqKindPluginIFList;
  }

  @Bean
  ReqKindService reqKindService(List<ReqKindPluginIF> reqKindPluginList) {
    return new ReqKindService(reqKindPluginList);
  }
//  @Bean
//  CacheReferenceAddressTagService cacheReferenceAddressTagService(
//     @NonNull RedisCacheService redisCacheService,
//     @NonNull RemoteAbstractTagService remoteAbstractTagService) {
//    return new CacheReferenceAddressTagService(redisCacheService, remoteAbstractTagService);
//  }
//
//  @Bean
//  CacheReferenceEventTagService cacheReferenceEventTagService(
//     @NonNull RedisCacheService redisCacheService,
//     @NonNull RemoteAbstractTagService remoteAbstractTagService) {
//    return new CacheReferenceEventTagService(redisCacheService, remoteAbstractTagService);
//  }

  @Bean("kindClassStringMap")
  public Map<Kind, String> kindClassStringMap() {
    ResourceBundle relaysBundle = ResourceBundle.getBundle("kind-class-map");
    return relaysBundle.keySet().stream()
       .collect(Collectors.toMap(Kind::valueOf, relaysBundle::getString));
  }

  @Bean("standardEventKindPlugins")
  List<StandardEventKindPlugin> standardEventKindPlugins() {
    return List.of();
  }

//  @Bean(name = "eventKindService")
//  EventKindService eventKindService(
//     @NonNull List<EventKindPluginIF> eventKindPlugins,
//     @NonNull @Qualifier("standardEventKindPlugins") List<StandardEventKindPlugin> standardEventKindPlugins) {
//    return new EventKindService(
//       Stream.concat(
//          eventKindPlugins.stream(),
//          standardEventKindPlugins.stream()).toList());
//  }

//  @Bean
//  EventPlugin eventPlugin(
//     @NonNull CacheServiceIF cacheServiceIF,
//     @NonNull @Qualifier("eventKindMaterializers") Map<Kind, Function<EventIF, Optional<? extends BaseEvent>>> eventKindMaterializers,
//     @NonNull @Qualifier("eventKindTypeMaterializers") Map<Kind, Function<EventIF, Optional<? extends BaseEvent>>> eventKindTypeMaterializers,
//     @NonNull @Qualifier("kindClassStringMap") Map<Kind, String> kindClassStringMap) {
//    return new EventPlugin(
//       cacheServiceIF,
//       eventKindMaterializers,
//       eventKindTypeMaterializers,
//       kindClassStringMap);
//  }

//  @Bean("eventKindMaterializers")
//  Map<Kind, Function<EventIF, Optional<? extends BaseEvent>>> eventKindMaterializers(
//     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
//     @NonNull CacheCuratedBadgeDefinitionGenericEventService cacheCuratedBadgeDefinitionGenericEventService,
//     @NonNull CacheBadgeSetsEventService cacheBadgeSetsEventService,
//     @NonNull CacheCuratedFormulaEventService cacheCuratedFormulaEventService,
//     @NonNull CacheCuratedBadgeAwardGenericEventService cacheCuratedBadgeAwardGenericEventService) {
//    Map<Kind, Function<EventIF, Optional<? extends BaseEvent>>> kindFxnMap = new HashMap<>();
//    kindFxnMap.put(
//       Kind.CURATION_SETS_BADGE_AWARD_EVENT,
//       eventIF ->
//          cacheCuratedBadgeAwardGenericEventService.materialize(eventIF));
//
//    kindFxnMap.put(
//       Kind.CURATION_SETS_BADGE_DEFINITION_EVENT,
//       eventIF ->
//          cacheCuratedBadgeDefinitionGenericEventService.materialize(eventIF));
//
//    kindFxnMap.put(
//       Kind.CURATION_SETS_FORMULA_EVENT,
//       eventIF ->
//          cacheCuratedFormulaEventService.materialize(eventIF));
//
//    kindFxnMap.put(
//       Kind.BADGE_AWARD_EVENT,
//       eventIF ->
//          cacheCuratedBadgeAwardGenericEventService.materialize(eventIF));
//
//    kindFxnMap.put(
//       Kind.BADGE_DEFINITION_EVENT,
//       eventIF ->
//          cacheCuratedBadgeDefinitionGenericEventService.materialize(eventIF));
//
//    kindFxnMap.put(
//       Kind.FOLLOW_SETS,
//       eventIF ->
//          cacheFollowSetsEventService.materialize(eventIF));
//
//    kindFxnMap.put(
//       Kind.BADGE_SETS_EVENT,
//       eventIF ->
//          cacheBadgeSetsEventService.materialize(eventIF));
//    return kindFxnMap;
//  }

//  @Bean("eventKindTypeMaterializers")
//  Map<Kind, Function<EventIF, Optional<? extends BaseEvent>>> eventKindTypeMaterializers(
//     @NonNull CacheBadgeAwardReputationEventService cacheBadgeAwardReputationEventService,
//     @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService) {
//    Map<Kind, Function<EventIF, Optional<? extends BaseEvent>>> kindFxnMap = new HashMap<>();
//
//    kindFxnMap.put(
//       Kind.BADGE_AWARD_EVENT,
//       eventIF ->
//          cacheBadgeAwardReputationEventService.materialize(eventIF));
//
//    kindFxnMap.put(
//       Kind.BADGE_DEFINITION_EVENT,
//       eventIF ->
//          cacheBadgeDefinitionReputationEventService.materialize(eventIF));
//
//    return kindFxnMap;
//  }
}
