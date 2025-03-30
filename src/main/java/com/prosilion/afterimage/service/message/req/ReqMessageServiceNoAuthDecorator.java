package com.prosilion.afterimage.service.message.req;

import com.prosilion.afterimage.service.clientresponse.ClientResponseService;
import com.prosilion.afterimage.service.message.MessageService;
import com.prosilion.afterimage.service.request.ReqService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.impl.GenericEvent;
import nostr.event.message.ReqMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(
    name = "afterimage.auth.active",
    havingValue = "false")
public class ReqMessageServiceNoAuthDecorator<T extends ReqMessage> implements MessageService<T> {
  private final ReqMessageService<T> reqMessageService;

  @Autowired
  public ReqMessageServiceNoAuthDecorator(
      ReqService<T, GenericEvent> reqService,
      ClientResponseService clientResponseService) {
    this.reqMessageService = new ReqMessageService<>(reqService, clientResponseService);
  }

  @Override
  public void processIncoming(@NonNull T reqMessage, @NonNull String sessionId) {
    log.debug("REQ decoded, contents: {}", reqMessage);
    reqMessageService.processIncoming(reqMessage, sessionId);
  }

  @Override
  public String getCommand() {
    return reqMessageService.getCommand();
  }
}
