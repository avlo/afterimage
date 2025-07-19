//package com.prosilion.afterimage.service.event.plugin;
//
//import com.prosilion.afterimage.relay.SuperconductorMeshProxy;
//import com.prosilion.nostr.enums.Kind;
//import com.prosilion.nostr.event.GenericEventKindIF;
//import com.prosilion.nostr.filter.Filterable;
//import com.prosilion.nostr.filter.Filters;
//import com.prosilion.nostr.filter.event.KindFilter;
//import com.prosilion.nostr.tag.IdentifierTag;
//import com.prosilion.nostr.tag.PubKeyTag;
//import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
//import com.prosilion.superconductor.base.service.event.type.EventEntityService;
//import com.prosilion.superconductor.base.service.event.type.NonPublishingEventKindPlugin;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.lang.NonNull;
//
//@Slf4j
//public class AImg39001NonPublishingEventFromAImg extends NonPublishingEventKindPlugin {
//  private final AImg30166EventFromAImgTypePlugin aImg30166EventFromAImgTypePlugin;
//  private final EventEntityService eventEntityService;
//
//  public AImg39001NonPublishingEventFromAImg(
//      @NonNull EventKindPluginIF<Kind> eventKindPlugin,
//      @NonNull EventEntityService eventEntityService,
//      @NonNull AImg30166EventFromAImgTypePlugin aImg30166EventFromAImgTypePlugin) {
//    super(eventKindPlugin);
//    this.aImg30166EventFromAImgTypePlugin = aImg30166EventFromAImgTypePlugin;
//    this.eventEntityService = eventEntityService;
//  }
//
////  start with pre-defined Map<String, String> afterimageRelays  
////  @Autowired
////  public AfterImageRelayAssimilationEventTypePlugin(
////      @NonNull RedisCache<T> redisCache,
////      @NonNull EventEntityService<T> eventEntityService,
////      @NonNull RelayDiscoveryEventTypePlugin<RelayDiscoveryEvent> relayDiscoveryEventTypePlugin,
////      @NonNull Identity aImgIdentity,
////      @NonNull Map<String, String> afterimageRelays) throws JsonProcessingException {
////    this(redisCache, eventEntityService, relayDiscoveryEventTypePlugin, aImgIdentity);
////    new SuperconductorMeshProxy<>(afterimageRelays, relayDiscoveryEventTypePlugin).setUpReputationReqFlux();
////  }
//
//  @SneakyThrows
//  @Override
//  public void processIncomingEvent(@NonNull GenericEventKindIF afterimageRelaysEvent) {
///*
//goal: incoming 39001 event makes aImg aware of other aImg relays (add impl class name here)
//{
//  "created_at": <Unix timestamp in seconds>,
//  "kind": 39001,	 <--------- NIP 29, group admins (AIMG mesh)
//  "tags": [
//    ["d", "afterimage_relay_mesh-url:port"], <------  group id
//    ["name", "<Community name>"],
//    ["description", "<Community description>"],
//
//    ["p", "<AIMG pubkey>", "<relay_1_URL>"],
//    ["p", "<AIMG pubkey>", "<relay_2_URL>"]
//  ],
//}
//*/
////   a. aImg queries it's DB for existing 30166 EVENT with dTag matching above relay_url
//    IdentifierTag inomingIdentifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, afterimageRelaysEvent).getFirst();
//    List<IdentifierTag> existingIdentifierTags = eventEntityService
//        .getEventsByKind(Kind.RELAY_DISCOVERY)
//        .stream().map(kind39001Event ->
//            Filterable.getTypeSpecificTags(IdentifierTag.class, kind39001Event))
//        .findFirst().orElseThrow();
//
////     a1. if existing 30166 event with same dTag exists, return (do nothing, we already have that aImg relay)
//    if (existingIdentifierTags.contains(inomingIdentifierTag))
//      return;
//
////     a2. else, for each new unique 39001 event pTags relay_url
////        submit ReqMessage containing Filter<Kind.30166 RELAY_DISCOVERY> to relay_url
//    Map<String, String> mapped =
//        Filterable.getTypeSpecificTags(
//                PubKeyTag.class, afterimageRelaysEvent).stream()
//            .collect(Collectors.toMap(pubKeyTag ->
//                    pubKeyTag.getPublicKey().toHexString(),
//                PubKeyTag::getMainRelayUrl));
//
//    new SuperconductorMeshProxy(mapped, getAbstractEventTypePlugin()).setUpReputationReqFlux(
//        new Filters(
//            new KindFilter(Kind.RELAY_DISCOVERY)));
//  }
//
//  EventKindPluginIF getAbstractEventTypePlugin() {
//    return aImg30166EventFromAImgTypePlugin;
//  }
//
//  @Override
//  public Kind getKind() {
//    return Kind.GROUP_ADMINS; // 39001 is list of an aImg relay's known other aImg relays
//  }
//}
