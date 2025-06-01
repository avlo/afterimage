package com.prosilion.afterimage.event.type;

import com.prosilion.afterimage.event.ReputationEvent;
import com.prosilion.superconductor.service.event.type.AbstractPublishingEventTypePlugin;
import com.prosilion.superconductor.service.event.type.RedisCache;
import com.prosilion.superconductor.service.request.NotifierService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.Kind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReputationEventTypePlugin<T extends ReputationEvent> extends AbstractPublishingEventTypePlugin<T> {

  @Autowired
  public ReputationEventTypePlugin(@NonNull RedisCache<T> redisCache, @NonNull NotifierService<T> notifierService) {
    super(redisCache, notifierService);
  }

  @Override
  public void processIncomingPublishingEventType(@NonNull T event) {
    log.debug("processing incoming REPUTATION event: [{}]", event);
    save(event);
  }

  @Override
  public Kind getKind() {
    return Kind.REPUTATION;
  }
}
