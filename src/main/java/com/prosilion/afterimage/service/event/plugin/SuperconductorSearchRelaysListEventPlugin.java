package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.user.Identity;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class SuperconductorSearchRelaysListEventPlugin extends AbstractRelayAnnouncementEventPlugin<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> {
  public SuperconductorSearchRelaysListEventPlugin(
      @NonNull Identity aImgIdentity,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull EventPlugin eventPlugin,
      @NonNull EventKindPluginIF eventKindPluginIF,
      @NonNull ReactiveRequestConsolidator reactiveRequestConsolidator) {
    super(
        aImgIdentity,
        cacheServiceIF,
        eventPlugin,
        eventKindPluginIF,
        reactiveRequestConsolidator);
  }

  @Override
  protected Filters getFilters() {
    log.debug("getFilters() of kind [{}]: {}",
        Kind.BADGE_AWARD_EVENT.getValue(),
        Kind.BADGE_AWARD_EVENT.getName().toUpperCase());
    return new Filters(new KindFilter(Kind.BADGE_AWARD_EVENT));
  }

  @Override
  public Kind getKind() {
    log.debug("getKind Kind[{}]: {}",
        Kind.SEARCH_RELAYS_LIST.getValue(),
        Kind.SEARCH_RELAYS_LIST.getName().toUpperCase());
    return Kind.SEARCH_RELAYS_LIST;
  }
}
