package com.prosilion.afterimage.service;

import com.prosilion.afterimage.client.SuperconductorMeshService;
import com.prosilion.superconductor.service.clientresponse.ClientResponseService;
import com.prosilion.superconductor.service.event.EventServiceIF;
import com.prosilion.superconductor.service.message.event.EventMessageServiceIF;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.impl.GenericEvent;
import nostr.event.message.EventMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SuperconductorEventMessageService<T extends EventMessage> implements EventMessageServiceIF<T> {
  private final EventServiceIF<GenericEvent> eventService;
  private final ClientResponseService clientResponseService;

  private final SuperconductorMeshService superconductorMeshService;

  @Autowired
  public SuperconductorEventMessageService(
      @NonNull SuperconductorMeshService superconductorMeshService,
      @NonNull EventServiceIF<GenericEvent> eventService,
      @NonNull ClientResponseService clientResponseService) {
    this.eventService = eventService;
    this.clientResponseService = clientResponseService;
    this.superconductorMeshService = superconductorMeshService;
  }

  @Override
  public void processIncoming(@NonNull T eventMessage, @NonNull String sessionId) {
//    received event from an SC instance and...
//    ...
//    then sends it to Mesh
    eventService.processIncomingEvent(eventMessage);
    processOkClientResponse(eventMessage, sessionId);
  }

  @Override
  public void processOkClientResponse(@NonNull T eventMessage, @NonNull String sessionId) {
    clientResponseService.processOkClientResponse(sessionId, eventMessage);
  }

  @Override
  public void processNotOkClientResponse(@NonNull T eventMessage, @NonNull String sessionId, @NonNull String errorMessage) {
    clientResponseService.processNotOkClientResponse(sessionId, new EventMessage(eventMessage.getEvent()), errorMessage);
  }
}
