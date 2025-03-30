package com.prosilion.afterimage.service.message.event;

import com.prosilion.afterimage.service.clientresponse.ClientResponseService;
import com.prosilion.afterimage.service.event.AuthEntityService;
import com.prosilion.afterimage.service.event.EventServiceIF;
import com.prosilion.afterimage.service.message.MessageService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.impl.GenericEvent;
import nostr.event.message.EventMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Slf4j
@Service
@ConditionalOnProperty(
    name = "afterimage.auth.active",
    havingValue = "true")
public class EventMessageServiceAuthDecorator<T extends EventMessage> implements MessageService<T> {
  private final EventMessageService<T> eventMessageService;
  private final AuthEntityService authEntityService;

  @Autowired
  public EventMessageServiceAuthDecorator(
      EventServiceIF<GenericEvent> eventService,
      ClientResponseService clientResponseService,
      AuthEntityService authEntityService) {
    this.eventMessageService = new EventMessageService<>(eventService, clientResponseService);
    this.authEntityService = authEntityService;
  }

  public void processIncoming(@NonNull T eventMessage, @NonNull String sessionId) {
    log.debug("AUTHENTICATED EVENT message NIP: {}", eventMessage.getNip());
    log.debug("AUTHENTICATED EVENT message type: {}", eventMessage.getEvent());
    try {
      authEntityService.findAuthEntityBySessionId(sessionId).orElseThrow();
    } catch (NoSuchElementException e) {
      log.debug("AUTHENTICATED EVENT message failed session authentication");
      eventMessageService.processNotOkClientResponse(eventMessage, sessionId, String.format("restricted: session [%s] has not been authenticated", sessionId));
      return;
    }
    eventMessageService.processIncoming(eventMessage, sessionId);
    eventMessageService.processOkClientResponse(eventMessage, sessionId);
  }

  @Override
  public String getCommand() {
    return eventMessageService.getCommand();
  }
}
