package com.prosilion.afterimage.service.event.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.service.RelayMeshProxy;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.service.EventKindServiceIF;
import com.prosilion.superconductor.base.service.event.service.EventKindTypeServiceIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class SuperconductorSearchRelaysListEventPlugin extends AbstractRelayAnnouncementEventPlugin { // Kind.SEARCH_RELAYS_LIST 10_007
  private final EventKindServiceIF eventKindServiceIF;

  public SuperconductorSearchRelaysListEventPlugin(
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull EventKindTypeServiceIF eventKindTypeService,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity) {
    super(eventKindPlugin, redisCacheServiceIF, aImgIdentity);
    this.eventKindServiceIF = eventKindTypeService;
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
    new RelayMeshProxy(
        uniqueNewRelays.stream().collect(
            Collectors.toMap(unused ->
                generateRandomHex64String(), relayUri ->
                Optional.of(relayUri).orElseThrow(() -> new InvalidTagException(relayUri, getKind().getName())))),
        eventKindServiceIF::processIncomingEvent).setUpRequestFlux(getFilters());
  }

  //  TODO: fix sneaky
  @SneakyThrows
  public BaseEvent createEvent(@NonNull Identity identity, @NonNull Stream<String> uniqueNewSuperconductorRelays) {
    log.debug("{} processing incoming Kind.SEARCH_RELAYS_LIST 10007 event", getClass().getSimpleName());
    return new SearchRelaysListEvent(
        identity,
        uniqueNewSuperconductorRelays.map(relayString ->
            new RelayTag(new Relay(relayString))).toList(),
        "Kind.SEARCH_RELAYS_LIST");
  }

  @Override
  protected Filters getFilters() {
    log.debug("{} getFilters() of Kind.BADGE_AWARD_EVENT", getClass().getSimpleName());
    return new Filters(new KindFilter(Kind.BADGE_AWARD_EVENT)); // kind 8
  }

  @Override
  public Kind getKind() {
    log.debug("{} getKind of Kind.SEARCH_RELAYS_LIST", getClass().getSimpleName());
    return Kind.SEARCH_RELAYS_LIST; // kind 10_007
  }
}
