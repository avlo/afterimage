package com.prosilion.afterimage.event.type;

import com.prosilion.superconductor.service.event.type.AbstractPublishingEventTypePlugin;
import com.prosilion.superconductor.service.event.type.RedisCache;
import com.prosilion.superconductor.service.request.NotifierService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.Kind;
import nostr.event.impl.GenericEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
// TODO: this class is necessary solely for its getKind() REPUTATION.  potential refactor
public class ReputationEventTypePlugin<T extends GenericEvent> extends AbstractPublishingEventTypePlugin<T> {

  @Autowired
  public ReputationEventTypePlugin(@NonNull RedisCache<T> redisCache, @NonNull NotifierService<T> notifierService) {
    super(redisCache, notifierService);
  }

  @Override
  public void processIncomingPublishingEventType(@NonNull T event) {
    log.debug("processing incoming REPUTATION event: [{}]", event);
  }

  @Override
  public Kind getKind() {
    return Kind.REPUTATION;
  }
}
