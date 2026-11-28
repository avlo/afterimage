package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.InvalidKindException;
import com.prosilion.afterimage.service.proxy.RelayMeshProxy;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.BlockedRelaysListEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.service.event.tag.RemoteAbstractTagService;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.DeleteEventServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.NonPublishingEventKindPlugin;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractRelayAnnouncementEventKindPlugin extends NonPublishingEventKindPlugin {
  protected final String superconductorRelayUrl;
  private final EventKindPluginIF eventKindPluginIF;
  protected final Identity aImgIdentity;
  protected final RemoteAbstractTagService remoteAbstractTagService;
  protected final CacheServiceIF cacheServiceIF;
  protected final DeleteEventServiceIF deleteEventServiceIF;

  public AbstractRelayAnnouncementEventKindPlugin(
     @NonNull String superconductorRelayUrl,
     @NonNull Identity aImgIdentity,
     @NonNull CacheServiceIF cacheServiceIF,
     @NonNull EventPlugin eventPlugin,
     @NonNull EventKindPluginIF eventKindPluginIF,
     @NonNull DeleteEventServiceIF deleteEventServiceIF) {
    super(eventPlugin);
    this.superconductorRelayUrl = superconductorRelayUrl;
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.eventKindPluginIF = eventKindPluginIF;
    this.deleteEventServiceIF = deleteEventServiceIF;
    this.remoteAbstractTagService = new RemoteAbstractTagService();
  }

  public Optional<GenericEventRecord> processIncomingEvent(@NonNull EventIF event, @NonNull Relay relay) {
    Optional<GenericEventRecord> eventByEventId = cacheServiceIF.getEventByEventId(event.getId());
    if (eventByEventId.isPresent())
      return eventByEventId;

    log.debug("processing incoming Kind[{}]:{}\n{}",
       event.getKind().getValue(),
       event.getKind().getName().toUpperCase(),
       event.createPrettyPrintJson());

    InvalidKindException.testBoolean(
       event.getKind().equals(getKind()),
       event.getKind().getName(), List.of(getKind().getName()));

    Optional<RelaysTag> relaysTag = preFilterFutureSearchRelayListEvents(event.requireFirstTag(RelaysTag.class));

    Set<Relay> existingKnownRelays = cacheServiceIF.getByKind(getKind()).stream()
       .limit(1)
       .flatMap(record -> record.requireFirstTag(RelaysTag.class)
          .getRelays().stream())
       .collect(Collectors.toSet());

    Set<Relay> uniqueNewRelays = relaysTag.map(RelaysTag::getRelays).stream()
       .flatMap(Collection::stream)
       .filter(incomingEventRelayTagRelay ->
          !existingKnownRelays.contains(incomingEventRelayTagRelay))
       .collect(Collectors.toSet());

    if (uniqueNewRelays.isEmpty()) {
      log.debug("did not discover any new unique relays, not saving incoming SearchRelaysList/RelaySets event, just return");
      return Optional.empty();
    }

//  saves new unique relays 
    Optional<GenericEventRecord> genericEventRecord = super.processIncomingEvent(
       createEvent(aImgIdentity, uniqueNewRelays),
       relay);

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

  protected Kind getSearchRelaysListEventKind() {
    log.debug("getKind searchRelaysListEventKind[{}]: {}",
       Kind.SEARCH_RELAYS_LIST.getValue(),
       Kind.SEARCH_RELAYS_LIST.getName().toUpperCase());
    return Kind.SEARCH_RELAYS_LIST;
  }

  protected Kind getBlockedRelaysListEventKind() {
    log.debug("getKind blockedRelaysListEventKind[{}]: {}",
       Kind.BLOCKED_RELAYS_LIST.getValue(),
       Kind.BLOCKED_RELAYS_LIST.getName().toUpperCase());
    return Kind.BLOCKED_RELAYS_LIST;
  }

  protected Optional<BlockedRelaysListEvent> getLocalBlockRelaysList() {
    List<GenericEventRecord> byKind = cacheServiceIF.getByKind(getBlockedRelaysListEventKind());
    Optional<BlockedRelaysListEvent> first = byKind.stream()
       .map(BlockedRelaysListEvent::new).findFirst();
    return first;
  }

  protected Optional<SearchRelaysListEvent> getLocalSearchRelaysList() {
    List<GenericEventRecord> byKind = cacheServiceIF.getByKind(getSearchRelaysListEventKind());
    Optional<SearchRelaysListEvent> first = byKind.stream()
       .map(SearchRelaysListEvent::new).findFirst();
    return first;
  }

  protected void delete(EventIF eventIF, String content) {
    log.debug("inside delete(eventIF, content) for event:\n {}", eventIF.createPrettyPrintJson());
    deleteEventServiceIF.processIncomingEvent(
       new DeletionEvent(
          aImgIdentity,
          new EventTag(eventIF.getId(), eventIF.getRelayTag().orElseThrow().getRelay().getUrl()),
          content,
          new Relay(superconductorRelayUrl)),
       new Relay(superconductorRelayUrl));
    log.debug("done.");
  }

  abstract protected Filters getFilters();

  abstract public Kind getKind();

  abstract protected BaseEvent createEvent(@NonNull Identity identity, @NonNull Set<Relay> uniqueNewRelays);

  abstract public Optional<RelaysTag> preFilterFutureSearchRelayListEvents(@NonNull RelaysTag remoteRelaysTag);
}
