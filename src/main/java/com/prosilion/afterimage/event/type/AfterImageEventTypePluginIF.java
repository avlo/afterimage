package com.prosilion.afterimage.event.type;

import com.prosilion.afterimage.util.SuperconductorMeshProxy;
import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventTypePlugin;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.EventTypePlugin;
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
import nostr.event.tag.PubKeyTag;
import nostr.id.Identity;
import org.apache.commons.lang3.stream.Streams;

@Slf4j
public abstract class AfterImageEventTypePluginIF<T extends GenericEvent> extends AbstractNonPublishingEventTypePlugin<T> {

  private final EventEntityService<T> eventEntityService;
  private final Identity aImgIdentity;

  public AfterImageEventTypePluginIF(
      @NonNull RedisCache<T> redisCache,
      @NonNull Identity aImgIdentity,
      @NonNull EventEntityService<T> eventEntityService) {
    super(redisCache);
    this.aImgIdentity = aImgIdentity;
    this.eventEntityService = eventEntityService;
  }

  @SneakyThrows
  @Override
  public void processIncomingNonPublishingEventType(@NonNull T afterimageRelaysEvent) {
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

    T newGroupAdminsEvent = createEvent(aImgIdentity, uniqueNewAfterimageRelays);
    save(newGroupAdminsEvent);

    Map<String, String> mapped =
        Filterable.getTypeSpecificTags(
                PubKeyTag.class, newGroupAdminsEvent).stream()
            .collect(Collectors.toMap(pubKeyTag ->
                    pubKeyTag.getPublicKey().toHexString(),
                PubKeyTag::getMainRelayUrl));

    new SuperconductorMeshProxy<>(mapped, getAbstractEventTypePlugin()).setUpReputationReqFlux();

//    Streams
//        .failableStream(
//            Filterable.getTypeSpecificTags(PubKeyTag.class, newContactListEvent))
//        .map(pubKeyTag -> Map.of(
//            pubKeyTag.getPublicKey().toHexString(), pubKeyTag.getMainRelayUrl()))
//        .forEach(relayNameUriMap ->
//            new SuperconductorMeshProxy<>(relayNameUriMap, relayDiscoveryEventTypePlugin).setUpReputationReqFlux());
  }

  abstract T createEvent(@NonNull Identity aImgIdentity, @NonNull List<BaseTag> tags);

  abstract public Kind getKind();

  abstract EventTypePlugin<T> getAbstractEventTypePlugin();
}
