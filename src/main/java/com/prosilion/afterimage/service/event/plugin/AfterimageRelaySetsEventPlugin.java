package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.service.RelayMeshProxyIF;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageRelaySetsEventPlugin extends AbstractRelayAnnouncementEventPlugin {
  public AfterimageRelaySetsEventPlugin(
      @NonNull Identity aImgIdentity,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull EventPlugin eventPlugin,
      @NonNull RelayMeshProxyIF reactiveRequestConsolidator) {
    super(aImgIdentity, cacheServiceIF, eventPlugin, reactiveRequestConsolidator);
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
