package com.prosilion.afterimage.service.event.plugin;

import com.google.common.collect.Sets;
import com.prosilion.afterimage.InvalidKindException;
import com.prosilion.afterimage.service.RelayMeshProxy;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.NonPublishingEventKindPlugin;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public abstract class AbstractRelayAnnouncementEventPlugin<T extends BaseEvent> extends NonPublishingEventKindPlugin {
  private final Identity aImgIdentity;
  private final CacheServiceIF cacheServiceIF;
  private final RelayMeshProxy relayMeshProxy;

  public AbstractRelayAnnouncementEventPlugin(
      @NonNull Identity aImgIdentity,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull EventPlugin eventPlugin,
      @NonNull EventKindPluginIF eventKindPluginIF,
      @NonNull ReactiveRequestConsolidator reactiveRequestConsolidator) {
    super(eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.relayMeshProxy = new RelayMeshProxy(
        eventKindPluginIF,
        reactiveRequestConsolidator);
  }

  public GenericEventRecord processIncomingEvent(EventIF event) {
    if (cacheServiceIF.getEventByEventId(event.getId()).isPresent())
      return event.asGenericEventRecord();

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

    if (uniqueNewRelays.isEmpty()) {
      log.debug("did not discover any new unique relays, not saving incoming SearchRelaysList/RelaySets event, just return");
      return event.asGenericEventRecord();
    }

    GenericEventRecord genericEventRecord = super.processIncomingEvent(event);

    log.debug("sending request to new relay(s):\n".concat(uniqueNewRelays.stream().map(s -> String.format("  %s", s)).collect(Collectors.joining(",\n"))));
    relayMeshProxy.setUpRequestFlux(getFilters(), uniqueNewRelays.stream().toList());

    return genericEventRecord;
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
