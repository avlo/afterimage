package com.prosilion.afterimage.service.event.plugin;

import com.google.common.collect.Sets;
import com.prosilion.afterimage.InvalidKindException;
import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.service.RelayMeshProxy;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.service.EventKindServiceIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.type.NonPublishingEventKindPlugin;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public abstract class AbstractRelayAnnouncementEventPlugin extends NonPublishingEventKindPlugin {
  private final EventKindServiceIF eventKindServiceIF;
  private final RedisCacheServiceIF redisCacheServiceIF;
  private final Identity aImgIdentity;

  public AbstractRelayAnnouncementEventPlugin(
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull EventKindServiceIF eventKindServiceIF,
      @NonNull Identity aImgIdentity) {
    super(eventKindPlugin);
    this.eventKindServiceIF = eventKindServiceIF;
    this.redisCacheServiceIF = redisCacheServiceIF;
    this.aImgIdentity = aImgIdentity;
  }

  @SneakyThrows
  @Override
  public void processIncomingEvent(@NonNull EventIF relaysEvent) {
    log.debug("processing incoming event: [{}]", relaysEvent);

    assert relaysEvent.getKind().equals(getKind()) : new InvalidKindException(relaysEvent.getKind().getName(), List.of(getKind().getName()));

    List<String> uniqueNewRelays = Sets.difference(
        Filterable.getTypeSpecificTags(RelayTag.class, relaysEvent).stream()
            .map(RelayTag::getRelay)
            .map(Relay::getUri)
            .map(URI::toString)
            .collect(Collectors.toSet()),
        redisCacheServiceIF.getByKind(getKind()).stream().map(eventDocumentIF ->
                eventDocumentIF.getTags().stream()
                    .map(RelayTag.class::cast)
                    .map(RelayTag::getRelay)
                    .map(Relay::getUri)
                    .map(URI::toString)
                    .toList())
            .flatMap(List::stream)
            .collect(Collectors.toSet())).stream().toList();

    if (uniqueNewRelays.isEmpty()) {
      log.debug("did not discover any new unique relays, so just return");
      return;
    }

    log.debug("uniqueNewRelays: [{}]", uniqueNewRelays);
    super.processIncomingEvent(createEvent(aImgIdentity, uniqueNewRelays));

    new RelayMeshProxy(
        uniqueNewRelays.stream().collect(
            Collectors.toMap(unused ->
                generateRandomHex64String(), relayUri ->
                Optional.of(relayUri).orElseThrow(() -> new InvalidTagException(relayUri, List.of(getKind().getName()))))),
        eventKindServiceIF::processIncomingEvent).setUpRequestFlux(getFilters());
  }

  abstract BaseEvent createEvent(@NonNull Identity identity, @NonNull List<String> uniqueNewRelays);

  abstract Filters getFilters();

  public abstract Kind getKind();

  private static String generateRandomHex64String() {
    return UUID.randomUUID().toString().concat(UUID.randomUUID().toString()).replaceAll("[^A-Za-z0-9]", "");
  }
}
