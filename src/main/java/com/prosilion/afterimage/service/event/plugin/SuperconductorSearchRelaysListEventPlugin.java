package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
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
public class SuperconductorSearchRelaysListEventPlugin extends AbstractRelayAnnouncementEventPlugin<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> {
  public SuperconductorSearchRelaysListEventPlugin(
      @NonNull Identity aImgIdentity,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull EventPlugin eventPlugin,
      @NonNull UniversalVoteEventPlugin universalVoteEventPlugin,
      @NonNull Function<EventIF, BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> eventMaterializer) {
    super(
        aImgIdentity,
        cacheServiceIF,
        eventPlugin,
        universalVoteEventPlugin,
        eventMaterializer::apply);
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
