package com.prosilion.afterimage.event.type;

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
import nostr.event.impl.GroupAdminsEvent;
import nostr.event.impl.RelayDiscoveryEvent;
import nostr.event.tag.PubKeyTag;
import nostr.id.Identity;
import org.apache.commons.lang3.stream.Streams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AfterImageRelayAssimilationEventTypePlugin<T extends GenericEvent> extends AbstractNonPublishingEventTypePlugin<T> {
  private final EventEntityService<T> eventEntityService;
  private final RelayDiscoveryEventTypePlugin<RelayDiscoveryEvent> relayDiscoveryEventTypePlugin;
  private final Identity aImgIdentity;

  @Autowired
  public AfterImageRelayAssimilationEventTypePlugin(
      @NonNull RedisCache<T> redisCache,
      @NonNull EventEntityService<T> eventEntityService,
      @NonNull RelayDiscoveryEventTypePlugin<RelayDiscoveryEvent> relayDiscoveryEventTypePlugin,
      @NonNull Identity aImgIdentity) {
    super(redisCache);
    this.eventEntityService = eventEntityService;
    this.relayDiscoveryEventTypePlugin = relayDiscoveryEventTypePlugin;
    this.aImgIdentity = aImgIdentity;
  }

  @SneakyThrows
  @Override
  public void processIncomingNonPublishingEventType(@NonNull T afterimageRelaysEvent) {
    log.debug("processing incoming AFTERIMAGE FOLLOWS LIST event: [{}]", afterimageRelaysEvent);

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

    T newGroupAdminsEvent = createGroupAdminsEvent(aImgIdentity, uniqueNewAfterimageRelays);
    save(newGroupAdminsEvent);

    Map<String, String> mapped =
        Filterable.getTypeSpecificTags(
                PubKeyTag.class, newGroupAdminsEvent).stream()
            .collect(Collectors.toMap(pubKeyTag ->
                    pubKeyTag.getPublicKey().toHexString(),
                PubKeyTag::getMainRelayUrl));

    new SuperconductorMeshProxy<>(mapped, relayDiscoveryEventTypePlugin).setUpReputationReqFlux();

//    Streams
//        .failableStream(
//            Filterable.getTypeSpecificTags(PubKeyTag.class, newContactListEvent))
//        .map(pubKeyTag -> Map.of(
//            pubKeyTag.getPublicKey().toHexString(), pubKeyTag.getMainRelayUrl()))
//        .forEach(relayNameUriMap ->
//            new SuperconductorMeshProxy<>(relayNameUriMap, relayDiscoveryEventTypePlugin).setUpReputationReqFlux());
  }

  private T createGroupAdminsEvent(@NonNull Identity identity, @NonNull List<BaseTag> tags) {
    T t = (T) new GroupAdminsEvent(
        identity.getPublicKey(),
        tags,
        "");
    identity.sign(t);
    return t;
  }

  @Override
  public Kind getKind() {
    return Kind.GROUP_ADMINS;
  }
}
