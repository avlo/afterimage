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
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventMaterializer;
import com.prosilion.superconductor.base.service.event.plugin.kind.NonPublishingEventKindPlugin;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;

@Slf4j
public abstract class AbstractRelayAnnouncementEventPlugin<T extends BaseEvent> extends NonPublishingEventKindPlugin {
  private final Identity aImgIdentity;
  private final CacheServiceIF cacheServiceIF;
  private final EventKindPluginIF eventKindPluginIF;
  private final EventMaterializer<T> eventMaterializer;
  
  public AbstractRelayAnnouncementEventPlugin(
      @NonNull Identity aImgIdentity,
      @NonNull @Qualifier("redisCacheService") CacheServiceIF cacheServiceIF,
      @NonNull @Qualifier("eventPlugin") EventPlugin eventPlugin,
      @NonNull EventKindPluginIF eventKindPluginIF,
      @NonNull EventMaterializer<T> eventMaterializer) {
    super(eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.eventKindPluginIF = eventKindPluginIF;
    this.eventMaterializer = eventMaterializer;
  }

  @SneakyThrows
  public void processIncomingEvent(EventIF event) {
    if (cacheServiceIF.getEventByEventId(event.getId()).isPresent())
      return;

    log.debug("processing incoming Kind[{}]:{}\n{}",
        event.getKind().getValue(),
        event.getKind().getName().toUpperCase(),
        event.createPrettyPrintJson());

    InvalidKindException.testBoolean(
        event.getKind().equals(getKind()),
        event.getKind().getName(), List.of(getKind().getName()));

    Set<String> eventRelays = getRelayTag(event)
        .collect(Collectors.toSet());

    Set<Stream<String>> existingKnownRelays = cacheServiceIF.getByKind(getKind()).stream()
        .map(AbstractRelayAnnouncementEventPlugin::getRelayTag)
        .collect(Collectors.toSet());

    Set<String> uniqueNewRelays = Sets.difference(
        eventRelays,
        existingKnownRelays);

    log.debug(
        uniqueNewRelays.isEmpty() ?
            "did not discover any new unique relays, so just return" :
            String.format("uniqueNewRelays:\n%s", uniqueNewRelays.stream().map(s -> String.format("  [%s]", s))));

    log.debug("processIncomingEventAuth(), unique new relays\n[{}]", uniqueNewRelays.stream()
        .map(s -> String.format("\n  %s", s))
        .map(s -> String.join(",", s)));

    Map<String, String> subscriberIdRelayMap = uniqueNewRelays.stream().collect(
        Collectors.toMap(unused ->
            generateRandomHex64String(), relayUri ->
            Optional.of(relayUri).orElseThrow(() -> new InvalidTagException(relayUri, Kind.RELAY_SETS.getName()))));

    RelayMeshProxy relayMeshProxy = new RelayMeshProxy(
        subscriberIdRelayMap,
        eventKindPluginIF,
        eventMaterializer);

    relayMeshProxy.setUpRequestFlux(getFilters());
    super.processIncomingEvent(event.asGenericEventRecord());
  }

  abstract protected Filters getFilters();

  public abstract Kind getKind();

  private static Stream<String> getRelayTag(EventIF eventIF) {
    return Filterable.getTypeSpecificTagsStream(RelaysTag.class, eventIF)
        .map(RelaysTag::getRelays)
        .flatMap(Collection::stream)
        .map(Relay::getUrl);
  }

  protected static String generateRandomHex64String() {
    return UUID.randomUUID().toString().concat(UUID.randomUUID().toString()).replaceAll("[^A-Za-z0-9]", "");
  }
}
