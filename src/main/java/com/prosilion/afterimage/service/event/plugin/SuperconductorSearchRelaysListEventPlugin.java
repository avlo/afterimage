package com.prosilion.afterimage.service.event.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.service.RelayMeshProxy;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
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
public class SuperconductorSearchRelaysListEventPlugin extends AbstractRelayAnnouncementEventPlugin { // Kind.SEARCH_RELAYS_LIST 10_007
  private final EventKindPluginIF universalVoteEventPlugin;

  public SuperconductorSearchRelaysListEventPlugin(
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull UniversalVoteEventPlugin universalVoteEventPlugin,
      @NonNull @Qualifier("redisCacheService") CacheServiceIF cacheServiceIF,
      @NonNull Identity aImgIdentity) {
    super(eventKindPlugin, cacheServiceIF, aImgIdentity);
    this.universalVoteEventPlugin = universalVoteEventPlugin;
  }

//  start with pre-defined Map<String, String> superconductorRelays
//  @Autowired
//  public SuperConductorRelayEnlistmentEventTypePlugin(
//      @NonNull RedisCache<GenericEventKindTypeIF> redisCache,
//      @NonNull EventEntityService<GenericEventKindTypeIF> eventEntityService,
//      @NonNull VoteEventTypePlugin<VoteEvent> voteEventTypePlugin,
//      @NonNull Identity aImgIdentity,
//      @NonNull Map<String, String> superconductorRelays) throws JsonProcessingException {
//    this(redisCache, eventEntityService, voteEventTypePlugin, aImgIdentity);
//    new SuperconductorMeshProxy<>(superconductorRelays, this.voteEventTypePlugin).setUpReputationReqFlux();
//  }

  public void processIncomingEventAuth(@NonNull Set<String> uniqueNewRelays) throws JsonProcessingException {
    log.debug("processIncomingEventAuth(), unique new relays\n[{}]", uniqueNewRelays.stream()
        .map(s -> String.format("\n  %s", s))
        .map(s -> String.join(",", s)));

    Map<String, String> subscriberIdRelayMap = uniqueNewRelays.stream().collect(
        Collectors.toMap(unused ->
            generateRandomHex64String(), relayUri ->
            Optional.of(relayUri).orElseThrow(() -> new InvalidTagException(relayUri, Kind.SEARCH_RELAYS_LIST.getName()))));
    RelayMeshProxy relayMeshProxy = new RelayMeshProxy(
        subscriberIdRelayMap, 
        universalVoteEventPlugin, universalVoteEventPlugin::materialize);

    Filters filters = getFilters();
    relayMeshProxy.setUpRequestFlux(filters);
  }

  //  TODO: fix sneaky
  @SneakyThrows
  public BaseEvent createEvent(@NonNull Identity identity, @NonNull Stream<String> relays) {
    log.debug("createEvent() using Kind[{}]: {}", Kind.SEARCH_RELAYS_LIST.getValue(), Kind.SEARCH_RELAYS_LIST.getName());
    return new SearchRelaysListEvent(identity, new RelaysTag(relays.map(Relay::new).toList()), "custom SEARCH_RELAYS_LIST content");
  }

  @Override
  public BaseEvent materialize(EventIF eventIF) {
    SearchRelaysListEvent searchRelaysListEvent = new SearchRelaysListEvent(eventIF.asGenericEventRecord());
    return searchRelaysListEvent;
  }

  @Override
  protected Filters getFilters() {
    log.debug("getFilters() of {} : {}}", Kind.BADGE_AWARD_EVENT.getName(), Kind.BADGE_AWARD_EVENT.getValue());
    return new Filters(new KindFilter(Kind.BADGE_AWARD_EVENT)); // kind 8
  }

  @Override
  public Kind getKind() {
    log.debug("getKind Kind[{}]: {}}",
        Kind.SEARCH_RELAYS_LIST.getValue(),
        Kind.SEARCH_RELAYS_LIST.getName());
    return Kind.SEARCH_RELAYS_LIST; // kind 10_007
  }
}
