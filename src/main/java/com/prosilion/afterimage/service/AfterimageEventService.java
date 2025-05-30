package com.prosilion.afterimage.service;

import com.prosilion.superconductor.service.event.EventServiceIF;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.EventTypeServiceIF;
import com.prosilion.superconductor.service.request.NotifierService;
import com.prosilion.superconductor.service.request.pubsub.AddNostrEvent;
import lombok.NonNull;
import nostr.event.impl.GenericEvent;
import nostr.event.message.EventMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AfterimageEventService<T extends GenericEvent> implements EventServiceIF<T> {
  private final NotifierService<T> notifierService;
  private final EventTypeServiceIF<T> eventTypeService;
  private final EventEntityService<T> eventEntityService;

  @Autowired
  public AfterimageEventService(
      @NonNull NotifierService<T> notifierService,
      @NonNull EventTypeServiceIF<T> eventTypeService,
      @NonNull EventEntityService<T> eventEntityService) {
    this.notifierService = notifierService;
    this.eventTypeService = eventTypeService;
    this.eventEntityService = eventEntityService;
  }

  public <U extends EventMessage> void processIncomingEvent(@NonNull U eventMessage) {
    T event = (T) eventMessage.getEvent();
    eventEntityService.saveEventEntity(event);
    eventTypeService.processIncomingEvent(event);
    notifierService.nostrEventHandler(new AddNostrEvent<>(event));
  }
}
