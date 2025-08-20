package com.prosilion.afterimage.service.event.plugin;

import com.google.common.collect.Sets;
import com.prosilion.afterimage.InvalidKindException;
import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.relay.SuperconductorMeshProxy;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.service.EventKindTypeServiceIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.type.NonPublishingEventKindPlugin;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class SuperconductorFollowsListNonPublishingEventKindPlugin extends NonPublishingEventKindPlugin {
  private final EventKindTypeServiceIF eventKindTypeService;
  private final RedisCacheServiceIF redisCacheServiceIF;
  private final Identity aImgIdentity;

  public SuperconductorFollowsListNonPublishingEventKindPlugin(
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull EventKindTypeServiceIF eventKindTypeService,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity) {
    super(eventKindPlugin);
    this.redisCacheServiceIF = redisCacheServiceIF;
    this.eventKindTypeService = eventKindTypeService;
    this.aImgIdentity = aImgIdentity;
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

  @SneakyThrows
  @Override
  public void processIncomingEvent(EventIF superconductorRelaysEvent) {
    log.debug("SuperConductorRelayEnlistmentNonPublishingEventTypePlugin processing incoming event: [{}]", superconductorRelaysEvent);

    assert superconductorRelaysEvent.getKind().equals(getKind()) : new InvalidKindException(superconductorRelaysEvent.getKind().getName(), List.of(getKind().getName()));

    Set<String> userSubmittedSuperconductorRelays = Filterable.getTypeSpecificTags(RelayTag.class, superconductorRelaysEvent)
        .stream()
        .map(RelayTag::getRelay)
        .map(Relay::getUri)
        .map(URI::toString)
        .collect(Collectors.toSet());

    Set<String> savedRelays = redisCacheServiceIF.getByKind(getKind())
        .stream()
        .map(e ->
            e.getTags()
                .stream()
                .map(RelayTag.class::cast)
                .map(RelayTag::getRelay)
                .map(Relay::getUri)
                .map(URI::toString).toList())
        .flatMap(List::stream).collect(Collectors.toSet());

    List<String> uniqueNewSuperconductorRelays = Sets.difference(userSubmittedSuperconductorRelays, savedRelays).stream().toList();

    if (uniqueNewSuperconductorRelays.isEmpty()) {
      log.debug("SuperConductorRelayEnlistmentNonPublishingEventTypePlugin processIncomingNonPublishingEventKind did not discover any new unique relays, so just return");
      return;
    }

    log.debug("SuperConductorRelayEnlistmentNonPublishingEventTypePlugin processIncomingNonPublishingEventKind uniqueNewSuperconductorRelays: [{}]", uniqueNewSuperconductorRelays);

    super.processIncomingEvent(
        createEvent(
            aImgIdentity,
            uniqueNewSuperconductorRelays));

    Map<String, String> mapped =
        uniqueNewSuperconductorRelays.stream()
            .collect(
                Collectors.toMap(
                    unused -> generateRandomHex64String(),
                    relayUri -> Optional.of(relayUri).orElseThrow(() ->
                        new InvalidTagException(relayUri, List.of(getKind().getName())))));

    new SuperconductorMeshProxy(mapped, eventKindTypeService::processIncomingEvent).setUpReputationReqFlux(getFilters());
  }

  //  TODO: fix sneaky
  @SneakyThrows
  public BaseEvent createEvent(@NonNull Identity identity, @NonNull List<String> uniqueNewSuperconductorRelays) {
    log.debug("SuperConductorRelayEnlistmentEventTypePlugin processing incoming Kind.SEARCH_RELAYS_LIST 10007 event");
    return new SearchRelaysListEvent(
        identity,
        uniqueNewSuperconductorRelays.stream().map(relayString ->
            new RelayTag(new Relay(relayString))).toList(),
        "Kind.RELAY_LIST_METADATA");
  }

  Filters getFilters() {
    log.debug("SuperConductorRelayEnlistmentEventTypePlugin getFilters() of Kind.BADGE_AWARD_EVENT");
    return new Filters(new KindFilter(Kind.BADGE_AWARD_EVENT));
  }

  @Override
  public Kind getKind() {
    log.debug("SuperConductorRelayEnlistmentEventTypePlugin getKind of Kind.SEARCH_RELAYS_LIST");
    return Kind.SEARCH_RELAYS_LIST;
  }

  public static String generateRandomHex64String() {
    return UUID.randomUUID().toString().concat(UUID.randomUUID().toString()).replaceAll("[^A-Za-z0-9]", "");
  }
}
