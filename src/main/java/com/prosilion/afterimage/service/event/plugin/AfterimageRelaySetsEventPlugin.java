package com.prosilion.afterimage.service.event.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.service.RelayMeshProxy;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.RelaySetsEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.service.EventKindServiceIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageRelaySetsEventPlugin extends AbstractRelayAnnouncementEventPlugin { // kind 30_002 "relays"
  private final EventKindServiceIF eventKindServiceIF;

  public AfterimageRelaySetsEventPlugin(
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull EventKindServiceIF eventKindServiceIF,
      @NonNull @Qualifier("redisCacheService") CacheServiceIF cacheServiceIF,
      @NonNull Identity aImgIdentity) {
    super(eventKindPlugin, cacheServiceIF, aImgIdentity);
    this.eventKindServiceIF = eventKindServiceIF;
  }

//  start with pre-defined Map<String, String> afterimageRelays  
//  @Autowired
//  public AfterImageRelayAssimilationEventTypePlugin(
//      @NonNull RedisCache<T> redisCache,
//      @NonNull EventEntityService<T> eventEntityService,
//      @NonNull RelayDiscoveryEventTypePlugin<RelayDiscoveryEvent> relayDiscoveryEventTypePlugin,
//      @NonNull Identity aImgIdentity,
//      @NonNull Map<String, String> afterimageRelays) throws JsonProcessingException {
//    this(redisCache, eventEntityService, relayDiscoveryEventTypePlugin, aImgIdentity);
//    new SuperconductorMeshProxy<>(afterimageRelays, relayDiscoveryEventTypePlugin).setUpReputationReqFlux();
//  }

  public void processIncomingEventAuth(@NonNull Set<String> uniqueNewRelays) throws JsonProcessingException {
    new RelayMeshProxy(
        uniqueNewRelays.stream().collect(
            Collectors.toMap(unused ->
                generateRandomHex64String(), relayUri ->
                Optional.of(relayUri).orElseThrow(() -> new InvalidTagException(relayUri, Kind.RELAY_SETS.getName())))),
        eventKindServiceIF::processIncomingEvent).setUpRequestFlux(getFilters());
  }

  //  TODO: fix sneaky
  @SneakyThrows
  @Override
  public BaseEvent createEvent(@NonNull Identity identity, @NonNull IdentifierTag identifierTag, @NonNull Stream<String> uniqueNewAImgRelays) {
    log.debug("{} processing incoming Kind.RELAY_SETS 30_002 event", getClass().getSimpleName());
    return new RelaySetsEvent(
        identity,
        identifierTag,
        uniqueNewAImgRelays.map(relayString ->
            new RelayTag(new Relay(relayString))).toList(),
        "Kind.RELAY_SETS");
  }

  @Override
  protected Filters getFilters() {
    log.debug("{} getFilters() of {} : {}}", getClass().getSimpleName(), Kind.FOLLOW_SETS.getName(), Kind.FOLLOW_SETS.getValue());
    return new Filters(new KindFilter(Kind.FOLLOW_SETS)); // kind 30_000 "p"
  }

  @Override
  public Kind getKind() {
    log.debug("{} getKind of {} : {}}", getClass().getSimpleName(), Kind.RELAY_SETS.getName(), Kind.RELAY_SETS.getValue());
    return Kind.RELAY_SETS; // kind 30_002 "relays"
  }
}
