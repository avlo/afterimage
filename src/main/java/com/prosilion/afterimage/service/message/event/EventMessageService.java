package com.prosilion.afterimage.service.message.event;

import com.prosilion.afterimage.service.clientresponse.ClientResponseService;
import com.prosilion.afterimage.service.event.EventServiceIF;
import com.prosilion.afterimage.service.message.MessageService;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.impl.GenericEvent;
import nostr.event.message.EventMessage;

@Slf4j
public class EventMessageService<T extends EventMessage> implements MessageService<T> {
  @Getter
  public final String command = "EVENT";
  private final EventServiceIF<GenericEvent> eventService;
  private final ClientResponseService clientResponseService;

  public EventMessageService(EventServiceIF<GenericEvent> eventService, ClientResponseService clientResponseService) {
    this.eventService = eventService;
    this.clientResponseService = clientResponseService;
  }

  public void processIncoming(@NonNull T eventMessage, @NonNull String sessionId) {
    eventService.processIncomingEvent(eventMessage);
  }

  protected void processOkClientResponse(@NonNull T eventMessage, @NonNull String sessionId) {
    clientResponseService.processOkClientResponse(sessionId, eventMessage);
  }

  protected void processNotOkClientResponse(@NonNull T eventMessage, @NonNull String sessionId, @NonNull String errorMessage) {
    clientResponseService.processNotOkClientResponse(sessionId, new EventMessage(eventMessage.getEvent()), errorMessage);
  }
}
