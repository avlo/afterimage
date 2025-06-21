package com.prosilion.afterimage.event.type;

import com.prosilion.afterimage.event.GroupMembersEvent;
import com.prosilion.afterimage.util.SuperconductorMeshProxy;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.NostrException;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.GenericEventKindIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.dto.EventDto;
import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventKindPlugin;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.RedisCache;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.stream.Streams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SuperConductorRelayEnlistmentNonPublishingEventTypePlugin extends AbstractNonPublishingEventKindPlugin {
  private final VoteEventKindTypePlugin voteEventKindTypePlugin;
  private final EventEntityService eventEntityService;
  private final Identity aImgIdentity;

  @Autowired
  public SuperConductorRelayEnlistmentNonPublishingEventTypePlugin(
      @NonNull RedisCache redisCache,
      @NonNull EventEntityService eventEntityService,
      @NonNull VoteEventKindTypePlugin voteEventKindTypePlugin,
      @NonNull Identity aImgIdentity) {
    super(redisCache);
    this.voteEventKindTypePlugin = voteEventKindTypePlugin;
    this.aImgIdentity = aImgIdentity;
    this.eventEntityService = eventEntityService;
  }

//  start with pre-defined Map<String, String> superconductorRelays
//  @Autowired
//  public SuperConductorRelayEnlistmentEventTypePlugin(
//      @NonNull RedisCache<GenericEventKindTypeIF> redisCache,
//      @NonNull EventEntityService<GenericEventKindTypeIF> eventEntityService,
//      @NonNull VoteEventTypePlugin<VoteEvent> voteEventTypePlugin,
//      @NonNull Identity aImgIdentity,
//      @NonNull Map<String, String> superconductorRelays) throws JsonProcessingException {
//    this(redisCache, eventEntityService, voteEventTypePlugin, aImgIdentity);
//    new SuperconductorMeshProxy<>(superconductorRelays, this.voteEventTypePlugin).setUpReputationReqFlux();
//  }

  @SneakyThrows
  public void processIncomingNonPublishingEventKind(@NonNull GenericEventKindIF afterimageRelaysEvent) {
    log.debug("processing incoming AFTERIMAGE event: [{}]", afterimageRelaysEvent);

    List<BaseTag> uniqueNewAfterimageRelays =
        Streams.failableStream(
                Filterable.getTypeSpecificTags(PubKeyTag.class, afterimageRelaysEvent))
            .filter(pubKeyTag ->
                !eventEntityService
                    .getEventsByKind(getKind())
                    .stream()
                    .map(savedEvent ->
                        Streams.failableStream(
                                Filterable.getTypeSpecificTags(PubKeyTag.class, savedEvent))
                            .stream()
                            .map(
                                PubKeyTag::getMainRelayUrl))
                    .flatMap(BaseStream::parallel).toList()
                    .contains(
                        pubKeyTag.getMainRelayUrl()))
            .stream()
            .map(BaseTag.class::cast)
            .toList();

    if (uniqueNewAfterimageRelays.isEmpty())
      return;

    GenericEventKindIF newGroupAdminsEvent = new EventDto(createEvent(aImgIdentity, uniqueNewAfterimageRelays)).convertBaseEventToDto();
    save(newGroupAdminsEvent);

    Map<String, String> mapped =
        Filterable.getTypeSpecificTags(
                PubKeyTag.class, newGroupAdminsEvent).stream()
            .collect(Collectors.toMap(pubKeyTag ->
                    pubKeyTag.getPublicKey().toHexString(),
                PubKeyTag::getMainRelayUrl));

    new SuperconductorMeshProxy(mapped, voteEventKindTypePlugin).setUpReputationReqFlux(getFilters());

//    Streams
//        .failableStream(
//            Filterable.getTypeSpecificTags(PubKeyTag.class, newContactListEvent))
//        .map(pubKeyTag -> Map.of(
//            pubKeyTag.getPublicKey().toHexString(), pubKeyTag.getMainRelayUrl()))
//        .forEach(relayNameUriMap ->
//            new SuperconductorMeshProxy<>(relayNameUriMap, relayDiscoveryEventTypePlugin).setUpReputationReqFlux());
  }

  public BaseEvent createEvent(@NonNull Identity identity, @NonNull List<BaseTag> uniqueNewAfterimageRelays) throws NostrException, NoSuchAlgorithmException {
    log.debug("SuperConductorRelayEnlistmentEventTypePlugin processing incoming Kind.GROUP_MEMBERS 39002 event");
    BaseEvent groupMembersEvent = new GroupMembersEvent(
        identity,
        getKind(),
        uniqueNewAfterimageRelays,
        "");
    return groupMembersEvent;
  }

  Filters getFilters() {
    return new Filters(new KindFilter(Kind.BADGE_AWARD_EVENT));
  }

  @Override
  public Kind getKind() {
    return Kind.GROUP_MEMBERS; // 39002 is list of an SC relay's known other SC relays
  }
}
