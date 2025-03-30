package com.prosilion.afterimage.service.event.type;

import lombok.NonNull;
import nostr.event.impl.GenericEvent;

public interface EventTypeServiceIF<T extends GenericEvent> {
  void processIncomingEvent(@NonNull T event);
}
