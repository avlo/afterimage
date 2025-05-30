package com.prosilion.afterimage.service;

import com.prosilion.afterimage.exception.InvalidReputationReqJsonException;
import com.prosilion.superconductor.service.clientresponse.ClientResponseService;
import com.prosilion.superconductor.service.message.req.ReqMessageServiceIF;
import com.prosilion.superconductor.service.request.ReqService;
import com.prosilion.superconductor.util.EmptyFiltersException;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.base.PublicKey;
import nostr.event.Kind;
import nostr.event.filter.AddressTagFilter;
import nostr.event.filter.Filters;
import nostr.event.filter.ReferencedPublicKeyFilter;
import nostr.event.impl.GenericEvent;
import nostr.event.message.ReqMessage;
import nostr.event.tag.AddressTag;
import nostr.event.tag.PubKeyTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReputationReqMessageService<T extends ReqMessage> implements ReqMessageServiceIF<T> {
  public static final String FILTER_KEY = ReferencedPublicKeyFilter.FILTER_KEY;
  private final ReqService<GenericEvent> reqService;
  private final ClientResponseService clientResponseService;
  private int counter = 0;

  @Autowired
  public ReputationReqMessageService(
//      TODO: since below two classes are already present in SC ReqMessageService @Bean, re-visit making this
//            class a decorator and passing ReqMessageService @Bean into this ctor()}}
      @NonNull ReqService<GenericEvent> reqService,
      @NonNull ClientResponseService clientResponseService) {
    log.debug("loaded ReputationReqMessageService bean");
    this.reqService = reqService;
    this.clientResponseService = clientResponseService;
  }

  @Override
  public void processIncoming(@NonNull T reqMessage, @NonNull String sessionId) {
//    TODO: note, below try/catch already handled in ReqMessageService @Bean as per decorator TODO, above
    try {
//      ReqMessage validReqMsg = new ReqMessage(reqMessage.getSubscriptionId(), validate(reqMessage));
      List<Filters> validatedUserRequestFilters = culledReferencedPubKeyFilters(reqMessage);
      ReqMessage validReqMsg = new ReqMessage(sessionId, validatedUserRequestFilters);
      reqService.processIncoming(validReqMsg, sessionId);
    } catch (InvalidReputationReqJsonException | EmptyFiltersException e) {
      processNoticeClientResponse(reqMessage, sessionId, e.getMessage());
    }
  }

  @Override
  public void processNoticeClientResponse(@NonNull T reqMessage, @NonNull String sessionId, @NonNull String errorMessage) {
    clientResponseService.processNoticeClientResponse(reqMessage, sessionId, errorMessage, false);
  }

  private List<Filters> culledReferencedPubKeyFilters(T reqMessage) throws InvalidReputationReqJsonException {
    List<PublicKey> filterables = validateRequiredFilterRxR(reqMessage);

    List<AddressTagFilter<AddressTag>> list = filterables.stream().map(publicKey ->
        new AddressTagFilter<>(new AddressTag(
            Kind.REPUTATION.getValue(),
            publicKey
//            , new IdentifierTag(String.format("REPUTATION_UUID-%s", counter++))
        ))).toList();

    List<Filters> list1 = list.stream().map(Filters::new).toList();
    return list1;
  }

  private List<Filters> validateRequiredFilter(T reqMessage) throws InvalidReputationReqJsonException {
    return Optional.of(
            reqMessage.getFiltersList().stream().map(filters ->
                new Filters(filters.getFilterByType(FILTER_KEY))).toList())
        .orElseThrow(() -> new InvalidReputationReqJsonException(reqMessage, FILTER_KEY));
  }

  private List<PublicKey> validateRequiredFilterRxR(T reqMessage) throws InvalidReputationReqJsonException {
    return Optional.of(reqMessage.getFiltersList().stream()
            .flatMap(filters -> filters.getFilterByType(FILTER_KEY).stream())
            .filter(PubKeyTag.class::isInstance)
            .map(PubKeyTag.class::cast)
            .map(PubKeyTag::getPublicKey)
            .toList())
        .orElseThrow(() -> new InvalidReputationReqJsonException(reqMessage, FILTER_KEY));
  }
}
