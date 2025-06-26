package com.prosilion.afterimage.request;

import com.prosilion.afterimage.util.InvalidReputationReqJsonException;
import com.prosilion.nostr.filter.AbstractFilterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.superconductor.service.request.ReqServiceIF;
import com.prosilion.superconductor.util.EmptyFiltersException;
import java.util.List;
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

  @Override
  public void processIncoming(@NonNull ReqMessage reqMessage, @NonNull String sessionId) throws EmptyFiltersException {
    List<Filters> filtersList = reqMessage.getFiltersList();

    List<KindFilter> list = filtersList.stream()
        .filter(filters ->
            !filters.getFilterByType(KindFilter.FILTER_KEY).isEmpty())
        .map(KindFilter.class::cast)
        .toList();

    list.stream().findAny()
        .orElseThrow(() ->
            new InvalidReputationReqJsonException(filtersList, KindFilter.FILTER_KEY));

//    reqKindService.getKinds().stream()
//        .filter(kinds::contains)
//        .findFirst()
//        .ifPresentOrElse(kindType ->
//            reqKindService.processIncoming(filtersList)),
//        () -> reqKindTypeService.processIncoming(filtersList);

    boolean foundMatchInReqKindService = reqKindService.getKinds().stream()
        .anyMatch(kind ->
            list.stream().map(AbstractFilterable::getFilterable).anyMatch(kind::equals));

    Filters f =
        foundMatchInReqKindService ?
            reqKindService.processIncoming(filtersList) :
            reqKindTypeService.processIncoming(filtersList);

    reqService.processIncoming(
        new ReqMessage(reqMessage.getSubscriptionId(), f), sessionId);
  }
}
