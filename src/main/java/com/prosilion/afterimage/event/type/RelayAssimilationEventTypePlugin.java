package com.prosilion.afterimage.event.type;

import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.EventTypePlugin;
import com.prosilion.superconductor.service.event.type.RedisCache;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseTag;
import nostr.event.Kind;
import nostr.event.impl.GenericEvent;
import nostr.event.impl.GroupAdminsEvent;
import nostr.event.impl.RelayDiscoveryEvent;
import nostr.id.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RelayAssimilationEventTypePlugin<T extends GenericEvent> extends AfterImageEventTypePluginIF<T> {
  private final RelayDiscoveryEventTypePlugin<RelayDiscoveryEvent> relayDiscoveryEventTypePlugin;

  @Autowired
  public RelayAssimilationEventTypePlugin(
      @NonNull RedisCache<T> redisCache,
      @NonNull EventEntityService<T> eventEntityService,
      @NonNull RelayDiscoveryEventTypePlugin<RelayDiscoveryEvent> relayDiscoveryEventTypePlugin,
      @NonNull Identity aImgIdentity) {
    super(redisCache, aImgIdentity, eventEntityService);
    this.relayDiscoveryEventTypePlugin = relayDiscoveryEventTypePlugin;
  }

//  start with pre-defined Map<String, String> afterimageRelays  
//  @Autowired
//  public AfterImageRelayAssimilationEventTypePlugin(
//      @NonNull RedisCache<T> redisCache,
//      @NonNull EventEntityService<T> eventEntityService,
//      @NonNull RelayDiscoveryEventTypePlugin<RelayDiscoveryEvent> relayDiscoveryEventTypePlugin,
//      @NonNull Identity aImgIdentity,
//      @NonNull Map<String, String> afterimageRelays) throws JsonProcessingException {
//    this(redisCache, eventEntityService, relayDiscoveryEventTypePlugin, aImgIdentity);
//    new SuperconductorMeshProxy<>(afterimageRelays, relayDiscoveryEventTypePlugin).setUpReputationReqFlux();
//  }

  @Override
  public T createEvent(@NonNull Identity aImgIdentity, @NonNull List<BaseTag> tags) {
    log.debug("processing incoming AfterImageRelayAssimilationEventTypePlugin event");
    T t = (T) new GroupAdminsEvent(
        aImgIdentity.getPublicKey(),
        tags,
        "");
    aImgIdentity.sign(t);
    return t;
  }

  @Override
  EventTypePlugin<T> getAbstractEventTypePlugin() {
    return (EventTypePlugin<T>) relayDiscoveryEventTypePlugin;
  }

  @Override
  public Kind getKind() {
    return Kind.GROUP_ADMINS;
  }
}
