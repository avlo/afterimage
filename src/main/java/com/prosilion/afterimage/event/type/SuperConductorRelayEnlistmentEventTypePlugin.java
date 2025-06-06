package com.prosilion.afterimage.event.type;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.util.SuperconductorMeshProxy;
import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventTypePlugin;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.RedisCache;
import java.util.List;
import java.util.Map;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseTag;
import nostr.event.Kind;
import nostr.event.filter.Filterable;
import nostr.event.impl.GenericEvent;
import nostr.event.impl.GroupMembersEvent;
import nostr.event.impl.VoteEvent;
import nostr.event.tag.PubKeyTag;
import nostr.id.Identity;
import org.apache.commons.lang3.stream.Streams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SuperConductorRelayEnlistmentEventTypePlugin<T extends GenericEvent> extends AbstractNonPublishingEventTypePlugin<T> {
  private final EventEntityService<T> eventEntityService;
  private final VoteEventTypePlugin<VoteEvent> voteEventTypePlugin;
  private final Identity aImgIdentity;

  @Autowired
  public SuperConductorRelayEnlistmentEventTypePlugin(
      @NonNull RedisCache<T> redisCache,
      @NonNull EventEntityService<T> eventEntityService,
      @NonNull VoteEventTypePlugin<VoteEvent> voteEventTypePlugin,
      @NonNull Identity aImgIdentity,
      @NonNull Map<String, String> superconductorRelays) throws JsonProcessingException {
    super(redisCache);
    this.eventEntityService = eventEntityService;
    this.voteEventTypePlugin = voteEventTypePlugin;
    this.aImgIdentity = aImgIdentity;
    new SuperconductorMeshProxy<>(superconductorRelays, this.voteEventTypePlugin).setUpReputationReqFlux();
  }

  @SneakyThrows
  @Override
  public void processIncomingNonPublishingEventType(@NonNull T superconductorEnlistmentEvent) {
    log.debug("processing incoming SUPERCONDUCTOR Group Members event: [{}]", superconductorEnlistmentEvent);

    List<BaseTag> uniqueNewSuperConductorRelays =
        Streams.failableStream(
                Filterable.getTypeSpecificTags(PubKeyTag.class, superconductorEnlistmentEvent))
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

    if (uniqueNewSuperConductorRelays.isEmpty())
      return;

    T newSuperconductorRelays = createGroupMembersEvent(aImgIdentity, uniqueNewSuperConductorRelays);
    save(newSuperconductorRelays);

    Map<String, String> mapped =
        Filterable.getTypeSpecificTags(
                PubKeyTag.class, newSuperconductorRelays).stream()
            .collect(Collectors.toMap(pubKeyTag ->
                    pubKeyTag.getPublicKey().toHexString(),
                PubKeyTag::getMainRelayUrl));

    new SuperconductorMeshProxy<>(mapped, voteEventTypePlugin).setUpReputationReqFlux();

//    Streams
//        .failableStream(
//            Filterable.getTypeSpecificTags(PubKeyTag.class, newSuperconductorRelays))
//        .map(pubKeyTag -> Map.of(
//            pubKeyTag.getPublicKey().toHexString(), pubKeyTag.getMainRelayUrl()))
//        .forEach(relayNameUriMap ->
//            new SuperconductorMeshProxy<>(relayNameUriMap, voteEventTypePlugin).setUpReputationReqFlux());
  }

  private T createGroupMembersEvent(@NonNull Identity identity, @NonNull List<BaseTag> tags) {
    T t = (T) new GroupMembersEvent(
        identity.getPublicKey(),
        tags,
        "");
    identity.sign(t);
    return t;
  }

  @Override
  public Kind getKind() {
    return Kind.GROUP_MEMBERS;
  }
}
