package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.RelaySetsEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.service.EventKindServiceIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageRelaySetsPlugin extends AbstractRelayAnnouncementPlugin {
  public AfterimageRelaySetsPlugin(
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull EventKindServiceIF eventKindServiceIF,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity) {
    super(eventKindPlugin, redisCacheServiceIF, eventKindServiceIF, aImgIdentity);
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
    log.debug("Aimg30002AimgMeshRelaySetsNonPublishingEvent processing incoming Kind.RELAY_SETS 30002 event");
    return new RelaySetsEvent(
        identity,
        uniqueNewAImgRelays.stream().map(relayString ->
            new RelayTag(new Relay(relayString))).toList(),
        "Kind.RELAY_SETS");
  }

  @Override
  Filters getFilters() {
    log.debug("Aimg30002AimgMeshRelaySetsNonPublishingEvent getFilters() of Kind.FOLLOW_SETS");
    return new Filters(new KindFilter(Kind.FOLLOW_SETS));
  }

  @Override
  public Kind getKind() {
    log.debug("Aimg30002AimgMeshRelaySetsNonPublishingEvent getKind of Kind.RELAY_SETS");
    return Kind.RELAY_SETS;
  }
}
