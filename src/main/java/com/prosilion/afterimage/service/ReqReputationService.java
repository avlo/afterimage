package com.prosilion.afterimage.service;

import com.prosilion.subdivisions.request.RequestConsolidator;
import com.prosilion.superconductor.service.clientresponse.ClientResponseService;
import com.prosilion.superconductor.service.event.EventService;
import com.prosilion.superconductor.service.message.req.ReqMessageServiceBean;
import com.prosilion.superconductor.service.request.ReqService;
import com.prosilion.superconductor.util.EmptyFiltersException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.impl.GenericEvent;
import nostr.event.message.ReqMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReqReputationService<T extends ReqMessage> implements ReqMessageServiceBean<T> {
  private final ReqService<GenericEvent> reqService;
  private final ClientResponseService clientResponseService;

  private final RequestConsolidator requestConsolidator;
  private final EventService<GenericEvent> eventService;

  @Autowired
  public ReqReputationService(
      @NonNull RequestConsolidator requestConsolidator,
      @NonNull EventService<GenericEvent> eventService,
      @NonNull ReqService<GenericEvent> reqService,
      @NonNull ClientResponseService clientResponseService) {
    log.debug("loaded ReqReputationService bean");
    this.reqService = reqService;
    this.clientResponseService = clientResponseService;
    this.requestConsolidator = requestConsolidator;
    this.eventService = eventService;
  }

  @Override
  public void processIncoming(@NonNull T reqMessage, @NonNull String sessionId) {
    try {
      reqService.processIncoming(reqMessage, sessionId);
    } catch (EmptyFiltersException e) {
      processNoticeClientResponse(reqMessage, sessionId, e.getMessage());
    }
  }

  public void processNoticeClientResponse(@NonNull T reqMessage, @NonNull String sessionId, @NonNull String errorMessage) {
    clientResponseService.processNoticeClientResponse(reqMessage, sessionId, errorMessage, false);
  }

  private void saveRetrievedEvents() {

  }
}
