package com.prosilion.afterimage.event.type;

import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventTypePlugin;
import com.prosilion.superconductor.service.event.type.RedisCache;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.Kind;
import nostr.event.impl.GenericEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RelayDiscoveryEventTypePlugin<T extends GenericEvent> extends AbstractNonPublishingEventTypePlugin<T> {
  private final SuperConductorRelayEnlistmentEventTypePlugin<T> plugin;

  @Autowired
  public RelayDiscoveryEventTypePlugin(
      @NonNull RedisCache<T> redisCache,
      @NonNull SuperConductorRelayEnlistmentEventTypePlugin<T> plugin) {
    super(redisCache);
    this.plugin = plugin;
  }

  @Override
  public void processIncomingNonPublishingEventType(@NonNull T relayDiscoveryEvent) {
    log.debug("processing incoming VOTE EVENT: [{}]", relayDiscoveryEvent);
    plugin.processIncomingEvent(relayDiscoveryEvent);
  }

  @Override
  public Kind getKind() {
    return Kind.RELAY_DISCOVERY;
  }
}
