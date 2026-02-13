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
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.kind.EventKindServiceIF;
import com.prosilion.superconductor.base.service.event.kind.type.EventKindTypeServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
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
  private final EventKindServiceIF eventKindServiceIF;

  public SuperconductorSearchRelaysListEventPlugin(
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull EventKindTypeServiceIF eventKindTypeService,
      @NonNull @Qualifier("redisCacheService") CacheServiceIF cacheServiceIF,
      @NonNull Identity aImgIdentity) {
    super(eventKindPlugin, cacheServiceIF, aImgIdentity);
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
    log.debug("processing incoming {} : {}", Kind.SEARCH_RELAYS_LIST.getName(), Kind.SEARCH_RELAYS_LIST.getValue());
    log.debug("unique new relays [{}]", uniqueNewRelays);
    new RelayMeshProxy(
        uniqueNewRelays.stream().collect(
            Collectors.toMap(unused ->
                generateRandomHex64String(), relayUri ->
                Optional.of(relayUri).orElseThrow(() -> new InvalidTagException(relayUri, Kind.SEARCH_RELAYS_LIST.getName())))),
        eventKindServiceIF::processIncomingEvent,
        eventIF -> new SearchRelaysListEvent(eventIF.asGenericEventRecord()))
        .setUpRequestFlux(getFilters());
  }

  //  TODO: fix sneaky
  @SneakyThrows
  public BaseEvent createEvent(@NonNull Identity identity, @NonNull IdentifierTag identifierTag, @NonNull Stream<String> uniqueNewSuperconductorRelays) {
    log.debug("processing incoming {} {} event", Kind.SEARCH_RELAYS_LIST.getName(), Kind.SEARCH_RELAYS_LIST.getValue());
    return new SearchRelaysListEvent(
        identity,
        uniqueNewSuperconductorRelays.map(relayString ->
            new RelaysTag(new Relay(relayString))).toList(),
        "Kind.SEARCH_RELAYS_LIST");
  }

  @Override
  public BaseEvent materialize(EventIF eventIF) {
    return new SearchRelaysListEvent(eventIF.asGenericEventRecord());
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
