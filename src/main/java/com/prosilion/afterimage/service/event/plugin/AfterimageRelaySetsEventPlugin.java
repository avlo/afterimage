package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.RelaySetsEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageRelaySetsEventPlugin extends AbstractRelayAnnouncementEventPlugin { // kind 30_002 "relays"
  public AfterimageRelaySetsEventPlugin(
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity) {
    super(eventKindPlugin, redisCacheServiceIF, aImgIdentity);
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

  //  TODO: fix sneaky
  @SneakyThrows
  @Override
  public BaseEvent createEvent(@NonNull Identity identity, @NonNull List<String> uniqueNewAImgRelays) {
    log.debug("{} processing incoming Kind.RELAY_SETS 30_002 event", getClass().getSimpleName());
    return new RelaySetsEvent(
        identity,
        uniqueNewAImgRelays.stream().map(relayString ->
            new RelayTag(new Relay(relayString))).toList(),
        "Kind.RELAY_SETS");
  }

  @Override
  Filters getFilters() {
    log.debug("{} getFilters() of Kind.FOLLOW_SETS", getClass().getSimpleName());
    return new Filters(new KindFilter(Kind.FOLLOW_SETS)); // kind 30_000 "p"
  }

  @Override
  public Kind getKind() {
    log.debug("{} getKind of Kind.RELAY_SETS", getClass().getSimpleName());
    return Kind.RELAY_SETS; // kind 30_002 "relays"
  }
}
