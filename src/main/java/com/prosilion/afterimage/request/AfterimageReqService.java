package com.prosilion.afterimage.request;

import com.prosilion.afterimage.util.InvalidReputationReqJsonException;
import com.prosilion.nostr.filter.AbstractFilterable;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.superconductor.service.request.ReqServiceIF;
import com.prosilion.superconductor.util.EmptyFiltersException;
import java.util.Collection;
import org.springframework.lang.NonNull;

public class AfterimageReqService implements ReqServiceIF {
  private final ReqServiceIF reqService;

  private final ReqKindServiceIF reqKindService;
  private final ReqKindTypeServiceIF reqKindTypeService;

  public AfterimageReqService(
      @NonNull ReqServiceIF reqService,
      @NonNull ReqKindServiceIF reqKindService,
      @NonNull ReqKindTypeServiceIF reqKindTypeService) {
    this.reqService = reqService;
    this.reqKindService = reqKindService;
    this.reqKindTypeService = reqKindTypeService;
  }

//  @Override
//  public void processIncoming(@NonNull ReqMessage reqMessage, @NonNull String sessionId) throws EmptyFiltersException {
//    List<Kind> kindFilterables = reqMessage.getFiltersList().stream()
//        .map(filters ->
//            filters.getFilterByType(KindFilter.FILTER_KEY))
//        .flatMap(Collection::stream)
//        .map(KindFilter.class::cast)
//        .map(AbstractFilterable::getFilterable).toList();
//
//    if (kindFilterables.isEmpty()) {
//      throw new InvalidReputationReqJsonException(reqMessage.getFiltersList(), KindFilter.FILTER_KEY);
//    }
//
//    reqService.processIncoming(
//        new ReqMessage(
//            reqMessage.getSubscriptionId(),
//            reqKindService.getKinds().stream().anyMatch(kind ->
//                kindFilterables.stream().anyMatch(kind::equals)) ?
//                reqKindService.processIncoming(reqMessage.getFiltersList()) :
//                reqKindTypeService.processIncoming(reqMessage.getFiltersList())), sessionId);
//  }

  @Override
  public void processIncoming(@NonNull ReqMessage reqMessage, @NonNull String sessionId) throws EmptyFiltersException {
    reqService.processIncoming(
        new ReqMessage(
            reqMessage.getSubscriptionId(),
            reqKindService.getKinds().stream()
                .anyMatch(kind ->
                    kind.equals(
                        reqMessage.getFiltersList().stream().map(filters ->
                                filters.getFilterByType(KindFilter.FILTER_KEY))
                            .flatMap(Collection::stream)
                            .map(KindFilter.class::cast)
                            .map(AbstractFilterable::getFilterable)
                            .findAny().orElseThrow(() ->
                                new InvalidReputationReqJsonException(reqMessage.getFiltersList(), KindFilter.FILTER_KEY)))) ?
                reqKindService.processIncoming(reqMessage.getFiltersList()) :
                reqKindTypeService.processIncoming(reqMessage.getFiltersList())), sessionId);
  }
}
