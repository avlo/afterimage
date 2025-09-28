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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    Set<String> uniqueNewRelays = Sets.difference(
        getRelayTag(relaysEvent)
            .collect(Collectors.toSet()),
        redisCacheServiceIF.getByKind(getKind()).stream()
            .map(AbstractRelayAnnouncementEventPlugin::getRelayTag)
            .collect(Collectors.toSet()));

    if (uniqueNewRelays.isEmpty()) {
      log.debug("did not discover any new unique relays, so just return");
      return;
    }

    log.debug("uniqueNewRelays: [{}]", uniqueNewRelays);
    super.processIncomingEvent(createEvent(aImgIdentity, uniqueNewRelays.stream()));

    new RelayMeshProxy(
        uniqueNewRelays.stream().collect(
            Collectors.toMap(unused ->
                generateRandomHex64String(), relayUri ->
                Optional.of(relayUri).orElseThrow(() -> new InvalidTagException(relayUri, getKind().getName())))),
        this::processIncomingEventAuth).setUpRequestFlux(getFilters());
  }

  public void processIncomingEventAuth(@NonNull EventIF relaysEvent) {
    eventKindServiceIF.processIncomingEvent(relaysEvent);
  }

  abstract BaseEvent createEvent(@NonNull Identity identity, @NonNull Stream<String> uniqueNewRelays);

  abstract Filters getFilters();

  public abstract Kind getKind();

  private static Stream<String> getRelayTag(EventIF eventIF) {
    return Filterable.getTypeSpecificTagsStream(RelayTag.class, eventIF)
        .map(RelayTag::getRelay)
        .map(Relay::getUri)
        .map(URI::toString);
  }

  private static String generateRandomHex64String() {
    return UUID.randomUUID().toString().concat(UUID.randomUUID().toString()).replaceAll("[^A-Za-z0-9]", "");
  }
}
