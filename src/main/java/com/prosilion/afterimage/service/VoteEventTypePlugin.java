package com.prosilion.afterimage.service;

import com.prosilion.superconductor.service.event.type.AbstractEventTypePlugin;
import com.prosilion.superconductor.service.event.type.RedisCache;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.Kind;
import nostr.event.impl.GenericEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VoteEventTypePlugin<T extends GenericEvent> extends AbstractEventTypePlugin<T> {
  @Autowired
  public VoteEventTypePlugin(@NonNull RedisCache<T> redisCache) {
    super(redisCache);
  }

  @Override
  public void processIncomingEvent(@NonNull T event) {
    log.debug("processing incoming REPUTATION EVENT: [{}]", event);
    save(event);
  }

  @Override
  public Kind getKind() {
    return Kind.VOTE;
  }
}
