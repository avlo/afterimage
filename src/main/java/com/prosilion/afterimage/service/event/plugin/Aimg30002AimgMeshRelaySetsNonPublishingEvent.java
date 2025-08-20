package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.relay.SuperconductorMeshProxy;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.type.NonPublishingEventKindPlugin;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class Aimg30002AimgMeshRelaySetsNonPublishingEvent extends NonPublishingEventKindPlugin {
  private final AImg30000ReputationSetsFromAImgTypePlugin aImg30000ReputationSetsFromAImgTypePlugin;
  private final RedisCacheServiceIF eventEntityService;

  public Aimg30002AimgMeshRelaySetsNonPublishingEvent(
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull RedisCacheServiceIF redisCacheService,
      @NonNull AImg30000ReputationSetsFromAImgTypePlugin aImg30000ReputationSetsFromAImgTypePlugin) {
    super(eventKindPlugin);
    this.aImg30000ReputationSetsFromAImgTypePlugin = aImg30000ReputationSetsFromAImgTypePlugin;
    this.eventEntityService = redisCacheService;
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

  @SneakyThrows
  @Override
  public void processIncomingEvent(@NonNull EventIF afterimageRelaysEvent) {
/*
goal: incoming 30002 event makes aImg aware of other aImg relays
{
  "kind": 30002,	 <--------- NIP 51, Relay Sets (AIMG mesh)
  "tags": [
    ["relay", "<relay_1_URL>"],
    ["relay", "<relay_1_URL>"],
    ...
  ],
}
*/
//   a. aImg queries it's DB for existing 30166 EVENT with dTag matching above relay_url
    IdentifierTag inomingIdentifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, afterimageRelaysEvent).getFirst();
    List<IdentifierTag> existingIdentifierTags = eventEntityService
        .getByKind(Kind.RELAY_DISCOVERY)
        .stream().map(kind39001Event ->
            Filterable.getTypeSpecificTags(IdentifierTag.class, kind39001Event))
        .findFirst().orElseThrow();

//     a1. if existing 30166 event with same dTag exists, return (do nothing, we already have that aImg relay)
    if (existingIdentifierTags.contains(inomingIdentifierTag))
      return;

//     a2. else, for each new unique 39001 event pTags relay_url
//        submit ReqMessage containing Filter<Kind.30166 RELAY_DISCOVERY> to relay_url
    Map<String, String> mapped =
        Filterable.getTypeSpecificTags(
                PubKeyTag.class, afterimageRelaysEvent).stream()
            .collect(Collectors.toMap(pubKeyTag ->
                    pubKeyTag.getPublicKey().toHexString(),
                pubKeyTag -> Optional.ofNullable(pubKeyTag.getMainRelayUrl()).orElseThrow()));

    new SuperconductorMeshProxy(mapped, getAbstractEventTypePlugin()).setUpReputationReqFlux(
        new Filters(
            new KindFilter(Kind.RELAY_DISCOVERY)));
  }

  EventKindPluginIF getAbstractEventTypePlugin() {
    return aImg30000ReputationSetsFromAImgTypePlugin;
  }

  @Override
  public Kind getKind() {
    return Kind.GROUP_ADMINS; // 39001 is list of an aImg relay's known other aImg relays
  }
}
