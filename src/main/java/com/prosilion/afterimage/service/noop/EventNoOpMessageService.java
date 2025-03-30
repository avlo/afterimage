package com.prosilion.afterimage.service.noop;

import com.prosilion.afterimage.service.clientresponse.ClientResponseService;
import com.prosilion.afterimage.service.message.MessageService;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.message.EventMessage;

@Slf4j
public class EventNoOpMessageService<T extends EventMessage> implements MessageService<T> {
  public final String noOp;

  @Getter
  public final String command = "EVENT";
  private final ClientResponseService clientResponseService;

  public EventNoOpMessageService(ClientResponseService clientResponseService, String noOp) {
    this.clientResponseService = clientResponseService;
    this.noOp = noOp;
  }

  @Override
  public void processIncoming(@NonNull T eventMessage, @NonNull String sessionId) {
    log.debug("processing incoming NOOP-EVENT: [{}]", eventMessage);
    clientResponseService.processNotOkClientResponse(sessionId, new EventMessage(eventMessage.getEvent()), noOp);
    clientResponseService.processCloseClientResponse(sessionId);
  }
}
