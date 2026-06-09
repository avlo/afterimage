package com.prosilion.afterimage.service.event.plugin;

import com.google.common.collect.Sets;
import com.prosilion.afterimage.InvalidKindException;
import com.prosilion.afterimage.service.RelayMeshProxy;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.NonPublishingEventKindPlugin;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public abstract class AbstractRelayAnnouncementEventPlugin extends NonPublishingEventKindPlugin {
  private final Identity aImgIdentity;
  private final CacheServiceIF cacheServiceIF;
  private final EventKindPluginIF eventKindPluginIF;

  public AbstractRelayAnnouncementEventPlugin(
     @NonNull Identity aImgIdentity,
     @NonNull CacheServiceIF cacheServiceIF,
     @NonNull EventPlugin eventPlugin,
     @NonNull EventKindPluginIF eventKindPluginIF) {
    super(eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.eventKindPluginIF = eventKindPluginIF;
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

    List<Relay> relaysTags = event.requireFirstTag(RelaysTag.class).getRelays();

    Set<String> existingKnownRelays = cacheServiceIF.getByKind(getKind()).stream()
       .map(EventIF::requireRelayTagUrl)
       .collect(Collectors.toSet());

    Set<String> uniqueNewRelays = Sets.difference(
       relaysTags.stream().map(Relay::getUrl).collect(Collectors.toSet()),
       existingKnownRelays);

    if (uniqueNewRelays.isEmpty()) {
      log.debug("did not discover any new unique relays, not saving incoming SearchRelaysList/RelaySets event, just return");
      return event.asGenericEventRecord();
    }

//  saves new unique relays 
    GenericEventRecord genericEventRecord = super.processIncomingEvent(createEvent(aImgIdentity, uniqueNewRelays));

    log.debug("RelayMeshProxy will send request filters:\n  [{}]\nto new relay(s):\n{}",
       getFilters().toString(2),
       uniqueNewRelays.stream()
          .map(s ->
             String.format("  [%s]", s))
          .collect(Collectors.joining(",\n")));

    log.debug("calling new RelayMeshProxy(eventKindPluginIF).activateRequestFlux(getFilters(), uniqueNewRelays) ...");
    log.debug("... using eventKindPluginIF type: [{}] ...", eventKindPluginIF.getClass().getSimpleName());
    new RelayMeshProxy(eventKindPluginIF).activateRequestFlux(getFilters(), uniqueNewRelays);

    return genericEventRecord;
  }

  abstract protected Filters getFilters();

  abstract public Kind getKind();

  abstract protected BaseEvent createEvent(@NonNull Identity identity, @NonNull Set<String> uniqueNewRelays);
}
