package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
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
public class AfterimageRelaySetsEventPlugin extends AbstractRelayAnnouncementEventPlugin<BadgeAwardReputationEvent> {
  public AfterimageRelaySetsEventPlugin(
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
        Kind.FOLLOW_SETS.getValue(),
        Kind.FOLLOW_SETS.getName().toUpperCase());
    return new Filters(new KindFilter(Kind.FOLLOW_SETS));
  }

  @Override
  public Kind getKind() {
    log.debug("getKind Kind[{}]: {}",
        Kind.RELAY_SETS.getValue(),
        Kind.RELAY_SETS.getName().toUpperCase());
    return Kind.RELAY_SETS;
  }
}
