//package com.prosilion.afterimage.service.event.plugin;
//
//import com.prosilion.afterimage.event.GroupMembersEvent;
//import com.prosilion.afterimage.relay.SuperconductorMeshProxy;
//import com.prosilion.nostr.enums.Kind;
//import com.prosilion.nostr.event.BaseEvent;
//import com.prosilion.nostr.event.GenericEventKindIF;
//import com.prosilion.nostr.filter.Filterable;
//import com.prosilion.nostr.filter.Filters;
//import com.prosilion.nostr.filter.event.KindFilter;
//import com.prosilion.nostr.tag.BaseTag;
//import com.prosilion.nostr.tag.PubKeyTag;
//import com.prosilion.nostr.user.Identity;
//import com.prosilion.superconductor.dto.GenericEventKindDto;
//import com.prosilion.superconductor.service.event.service.plugin.EventKindPluginIF;
//import com.prosilion.superconductor.service.event.type.EventEntityService;
//import com.prosilion.superconductor.service.event.type.NonPublishingEventKindPlugin;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.stream.BaseStream;
//import java.util.stream.Collectors;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.stream.Streams;
//import org.springframework.lang.NonNull;
//
//@Slf4j
//public class SuperConductorRelayEnlistmentNonPublishingEventKindPlugin extends NonPublishingEventKindPlugin {
//  //  private final List<VoteEventKindTypePlugin> voteEventKindTypePlugins;
//  private final EventEntityService eventEntityService;
//  private final Identity aImgIdentity;
//
//  public SuperConductorRelayEnlistmentNonPublishingEventKindPlugin(
//      @NonNull EventKindPluginIF<Kind> eventKindPlugin,
////      @NonNull List<VoteEventKindTypePlugin> voteEventKindTypePlugins,
//      @NonNull EventEntityService eventEntityService,
//      @NonNull Identity aImgIdentity) {
//    super(eventKindPlugin);
//    this.eventEntityService = eventEntityService;
////    this.voteEventKindTypePlugins = voteEventKindTypePlugins;
//    this.aImgIdentity = aImgIdentity;
//  }
//
////  start with pre-defined Map<String, String> superconductorRelays
////  @Autowired
////  public SuperConductorRelayEnlistmentEventTypePlugin(
////      @NonNull RedisCache<GenericEventKindTypeIF> redisCache,
////      @NonNull EventEntityService<GenericEventKindTypeIF> eventEntityService,
////      @NonNull VoteEventTypePlugin<VoteEvent> voteEventTypePlugin,
////      @NonNull Identity aImgIdentity,
////      @NonNull Map<String, String> superconductorRelays) throws JsonProcessingException {
////    this(redisCache, eventEntityService, voteEventTypePlugin, aImgIdentity);
////    new SuperconductorMeshProxy<>(superconductorRelays, this.voteEventTypePlugin).setUpReputationReqFlux();
////  }
//
//
//  @SneakyThrows
//  @Override
//  public void processIncomingEvent(GenericEventKindIF afterimageRelaysEvent) {
//    log.debug("SuperConductorRelayEnlistmentNonPublishingEventTypePlugin processing incoming event: [{}]", afterimageRelaysEvent);
//
//    List<BaseTag> uniqueNewAfterimageRelays =
//        Streams.failableStream(
//                Filterable.getTypeSpecificTags(PubKeyTag.class, afterimageRelaysEvent))
//            .filter(pubKeyTag ->
//                !eventEntityService
//                    .getEventsByKind(getKind())
//                    .stream()
//                    .map(savedEvent ->
//                        Streams.failableStream(
//                                Filterable.getTypeSpecificTags(PubKeyTag.class, savedEvent))
//                            .stream()
//                            .map(
//                                PubKeyTag::getMainRelayUrl))
//                    .flatMap(BaseStream::parallel).toList()
//                    .contains(
//                        pubKeyTag.getMainRelayUrl()))
//            .stream()
//            .map(BaseTag.class::cast)
//            .toList();
//
//    if (uniqueNewAfterimageRelays.isEmpty()) {
//      log.debug("SuperConductorRelayEnlistmentNonPublishingEventTypePlugin processIncomingNonPublishingEventKind did not discover any new unique relays, so just return");
//      return;
//    }
//
//    log.debug("SuperConductorRelayEnlistmentNonPublishingEventTypePlugin processIncomingNonPublishingEventKind uniqueNewAfterimageRelays: [{}]", uniqueNewAfterimageRelays);
//
//    GenericEventKindIF newGroupAdminsEvent = new GenericEventKindDto(createEvent(aImgIdentity, uniqueNewAfterimageRelays)).convertBaseEventToGenericEventKindIF();
//    super.processIncomingEvent(newGroupAdminsEvent);
//
//    Map<String, String> mapped =
//        Filterable.getTypeSpecificTags(
//                PubKeyTag.class, newGroupAdminsEvent).stream()
//            .collect(
//                Collectors.toMap(
//                    pubKeyTag -> pubKeyTag.getPublicKey().toHexString(),
//                    pubKeyTag -> Optional.ofNullable(pubKeyTag.getMainRelayUrl()).orElseThrow(() ->
//                        new IllegalArgumentException("SuperConductorRelayEnlistmentNonPublishingEventTypePlugin processIncomingNonPublishingEventKind() newGroupAdminsEvent's PubKeyTag must include a non-null & non-empty URL"))));
//
//    new SuperconductorMeshProxy(mapped, this).setUpReputationReqFlux(getFilters());
//  }
//
//  //  TODO: fix sneaky
//  @SneakyThrows
//  public BaseEvent createEvent(@NonNull Identity identity, @NonNull List<BaseTag> uniqueNewAfterimageRelays) {
//    log.debug("SuperConductorRelayEnlistmentEventTypePlugin processing incoming Kind.GROUP_MEMBERS 39002 event");
//    BaseEvent groupMembersEvent = new GroupMembersEvent(
//        identity,
//        getKind(),
//        uniqueNewAfterimageRelays,
//        "");
//    return groupMembersEvent;
//  }
//
//  Filters getFilters() {
//    log.debug("SuperConductorRelayEnlistmentEventTypePlugin getFilters() of Kind.BADGE_AWARD_EVENT");
//    return new Filters(new KindFilter(Kind.BADGE_AWARD_EVENT));
//  }
//
//  @Override
//  public Kind getKind() {
//    log.debug("SuperConductorRelayEnlistmentEventTypePlugin getKind of Kind.GROUP_MEMBERS");
//    return Kind.GROUP_MEMBERS; // 39002 is list of an SC relay's known other SC relays
//  }
//}
