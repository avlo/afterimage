package com.prosilion.afterimage.service;

import com.prosilion.afterimage.exception.InvalidReputationReqJsonException;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import com.prosilion.superconductor.service.clientresponse.ClientResponseService;
import com.prosilion.superconductor.service.message.req.ReqMessageServiceIF;
import com.prosilion.superconductor.service.request.ReqService;
import com.prosilion.superconductor.util.EmptyFiltersException;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.filter.Filters;
import nostr.event.filter.ReferencedPublicKeyFilter;
import nostr.event.filter.VoteTagFilter;
import nostr.event.impl.GenericEvent;
import nostr.event.message.ReqMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReputationReqMessageService<T extends ReqMessage> implements ReqMessageServiceIF<T> {
  private final ReqService<GenericEvent> reqService;
  private final ClientResponseService clientResponseService;

  private final ReactiveRequestConsolidator reactiveRequestConsolidator;

  @Autowired
  public ReputationReqMessageService(
      @NonNull ReactiveRequestConsolidator reactiveRequestConsolidator,
//      TODO: since below two classes are already present in SC ReqMessageService @Bean, re-visit making this
//            class a decorator and passing ReqMessageService @Bean into this ctor()}}
      @NonNull ReqService<GenericEvent> reqService,
      @NonNull ClientResponseService clientResponseService) {
    log.debug("loaded ReputationReqMessageService bean");
    this.reqService = reqService;
    this.clientResponseService = clientResponseService;
    this.reactiveRequestConsolidator = reactiveRequestConsolidator;
  }

  @Override
  public void processIncoming(@NonNull T reqMessage, @NonNull String sessionId) {
//    TODO: note, below try/catch already handled in ReqMessageService @Bean as per decorator TODO, above
    try {
      ReqMessage validReqMessage = new ReqMessage(
          reqMessage.getSubscriptionId(),
          getValidatedFilters(reqMessage));
      reqService.processIncoming(validReqMessage, sessionId);
    } catch (InvalidReputationReqJsonException | EmptyFiltersException e) {
      processNoticeClientResponse(reqMessage, sessionId, e.getMessage());
    }
  }

  public void processNoticeClientResponse(@NonNull T reqMessage, @NonNull String sessionId, @NonNull String errorMessage) {
    clientResponseService.processNoticeClientResponse(reqMessage, sessionId, errorMessage, false);
  }

  private List<Filters> getValidatedFilters(T reqMessage) throws InvalidReputationReqJsonException {
    validateRequiredFilter(reqMessage, ReferencedPublicKeyFilter.FILTER_KEY);
    validateRequiredFilter(reqMessage, VoteTagFilter.FILTER_KEY);
    return reqMessage.getFiltersList();
  }

  private void validateRequiredFilter(T reqMessage, String filterType) throws InvalidReputationReqJsonException {
    reqMessage.getFiltersList().stream()
        .flatMap(filters -> filters.getFilterByType(filterType).stream())
        .findFirst().orElseThrow(() -> new InvalidReputationReqJsonException(reqMessage, filterType));
  }
}
