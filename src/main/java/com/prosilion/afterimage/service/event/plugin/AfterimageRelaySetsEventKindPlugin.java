package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.RelaySetsEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AfterimageRelaySetsEventKindPlugin extends AbstractRelayAnnouncementEventKindPlugin {
  public AfterimageRelaySetsEventKindPlugin(
     @NonNull Identity aImgIdentity,
     @NonNull CacheServiceIF cacheServiceIF,
     @NonNull EventPlugin eventPlugin,
     @NonNull EventKindPluginIF eventKindPluginIF) {
    super(aImgIdentity, cacheServiceIF, eventPlugin, eventKindPluginIF);
  }

  @Override
  public Optional<GenericEventRecord> processIncomingEvent(@NonNull EventIF event, @NonNull Relay relay) {
    log.debug("inside processIncomingEvent(EventIF):\n  {}", event.createPrettyPrintJson());
    Optional<GenericEventRecord> genericEventRecord = super.processIncomingEvent(event, relay);
    log.debug("call to super.processIncomingEvent(...) returned genericEventRecord:\n  {}", genericEventRecord.map(GenericEventRecord::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));
    return genericEventRecord;
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

  @Override
  protected BaseEvent createEvent(@NonNull Identity identity, @NonNull Set<Relay> uniqueNewRelays) {
    RelaySetsEvent event = new RelaySetsEvent(
       identity,
       new RelaysTag(uniqueNewRelays),
       "AfterimageRelaySetsEventPlugin created RelaySetsEvent");
    log.debug("createEvent(..., @NonNull Set<String> uniqueNewRelays):\n {}", event.createPrettyPrintJson());
    return event;
  }
}
