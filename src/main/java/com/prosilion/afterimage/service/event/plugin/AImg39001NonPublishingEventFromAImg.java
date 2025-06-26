//package com.prosilion.afterimage.event.type;
//
//import com.prosilion.afterimage.util.SuperconductorMeshProxy;
//import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventTypePlugin;
//import com.prosilion.superconductor.service.event.type.EventEntityService;
//import com.prosilion.superconductor.service.event.type.EventTypePlugin;
//import com.prosilion.superconductor.service.event.type.RedisCache;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//import lombok.NonNull;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import nostr.event.Kind;
//import nostr.event.filter.Filterable;
//import nostr.event.filter.Filters;
//import nostr.event.filter.KindFilter;
//import nostr.event.impl.GenericEvent;
//import nostr.event.impl.RelayDiscoveryEvent;
//import nostr.event.tag.IdentifierTag;
//import nostr.event.tag.PubKeyTag;
//import nostr.id.Identity;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class AImg39001NonPublishingEventFromAImg<T extends GenericEvent> extends AbstractNonPublishingEventTypePlugin<T> {
//  private final AImg30166EventFromAImgTypePlugin<RelayDiscoveryEvent> AImg30166EventFromAImgTypePlugin;
//  private final EventEntityService<T> eventEntityService;
//
//  @Autowired
//  public AImg39001NonPublishingEventFromAImg(
//      @NonNull RedisCache<T> redisCache,
//      @NonNull EventEntityService<T> eventEntityService,
//      @NonNull AImg30166EventFromAImgTypePlugin<RelayDiscoveryEvent> AImg30166EventFromAImgTypePlugin) {
//    super(redisCache);
//    this.AImg30166EventFromAImgTypePlugin = AImg30166EventFromAImgTypePlugin;
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
//  public void processIncomingNonPublishingEventType(@NonNull T afterimageRelaysEvent) {
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
//    new SuperconductorMeshProxy<>(mapped, getAbstractEventTypePlugin()).setUpReputationReqFlux(
//        new Filters(
//            new KindFilter<>(Kind.RELAY_DISCOVERY)));
//  }
//
////  @Override
////  public T createEvent(@NonNull Identity aImgIdentity, @NonNull List<BaseTag> tags) {
////    log.debug("processing incoming AfterImageRelayAssimilationEventTypePlugin event");
////    T t = (T) new GroupAdminsEvent(
////        aImgIdentity.getPublicKey(),
////        tags,
////        "");
////    aImgIdentity.sign(t);
////    return t;
////  }
//
//  EventTypePlugin<T> getAbstractEventTypePlugin() {
//    return (EventTypePlugin<T>) AImg30166EventFromAImgTypePlugin;
//  }
//
//  @Override
//  public Kind getKind() {
//    return Kind.GROUP_ADMINS; // 39001 is list of an aImg relay's known other aImg relays
//  }
//}
