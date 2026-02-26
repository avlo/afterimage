package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageRelaySetsEventPlugin extends AbstractRelayAnnouncementEventPlugin<BadgeAwardReputationEvent> {
  public AfterimageRelaySetsEventPlugin(
      @NonNull Identity aImgIdentity,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull EventPlugin eventPlugin,
      @NonNull AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin,
      @NonNull Function<EventIF, BadgeAwardReputationEvent> eventMaterializer) {
    super(
        aImgIdentity,
        cacheServiceIF,
        eventPlugin,
        afterimageFollowSetsEventPlugin,
        eventMaterializer::apply);
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
