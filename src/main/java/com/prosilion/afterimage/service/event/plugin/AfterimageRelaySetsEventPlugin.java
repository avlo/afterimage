package com.prosilion.afterimage.service.event.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.service.RelayMeshProxy;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.RelaySetsEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.kind.EventKindServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import java.util.Map;
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
    log.debug("processIncomingEventAuth(), unique new relays\n[{}]", uniqueNewRelays.stream()
        .map(s -> String.format("\n  %s", s))
        .map(s -> String.join(",", s)));

    Map<String, String> subscriberIdRelayMap = uniqueNewRelays.stream().collect(
        Collectors.toMap(unused ->
            generateRandomHex64String(), relayUri ->
            Optional.of(relayUri).orElseThrow(() -> new InvalidTagException(relayUri, Kind.RELAY_SETS.getName()))));

    new RelayMeshProxy(subscriberIdRelayMap, eventKindServiceIF::processIncomingEvent, this::materialize)
        .setUpRequestFlux(getFilters());
  }

  //  TODO: fix sneaky
  @SneakyThrows
  @Override
  public BaseEvent createEvent(@NonNull Identity identity, @NonNull Stream<String> uniqueNewAImgRelays) {
    log.debug("createEvent() using Kind[{}]: {}}", Kind.RELAY_SETS.getValue(), Kind.RELAY_SETS.getName());
    return new RelaySetsEvent(identity, new RelaysTag(uniqueNewAImgRelays.map(Relay::new).toList()), "Kind.RELAY_SETS");
  }

  @Override
  public RelaySetsEvent materialize(EventIF eventIF) {
    return new RelaySetsEvent(eventIF.asGenericEventRecord());
  }

  @Override
  protected Filters getFilters() {
    log.debug("getFilters() of {} : {}}", Kind.FOLLOW_SETS.getName(), Kind.FOLLOW_SETS.getValue());
    return new Filters(new KindFilter(Kind.FOLLOW_SETS)); // kind 30_000 "p"
  }

  @Override
  public Kind getKind() {
    log.debug("getKind Kind[{}]: {}}",
        Kind.RELAY_SETS.getValue(),
        Kind.RELAY_SETS.getName());
    return Kind.RELAY_SETS; // kind 30_002 "relays"
  }
}
