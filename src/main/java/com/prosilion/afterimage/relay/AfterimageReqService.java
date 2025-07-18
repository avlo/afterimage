package com.prosilion.afterimage.relay;

import com.prosilion.afterimage.InvalidReputationReqJsonException;
import com.prosilion.afterimage.service.request.ReqKindServiceIF;
import com.prosilion.afterimage.service.request.ReqKindTypeServiceIF;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.filter.AbstractFilterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.superconductor.base.service.request.ReqServiceIF;
import java.util.Collection;
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
  public void processIncoming(@NonNull ReqMessage reqMessage, @NonNull String sessionId) throws NostrException {
// TODO: refactor when testing complete    
    List<Filters> filtersList = validateFiltersExist(reqMessage.getFiltersList());

    ReqMessage reqMessage1 = new ReqMessage(
        reqMessage.getSubscriptionId(),
        reqKindService.getKinds().stream()
            .anyMatch(kind ->
                kind.equals(
                    filtersList.stream()
                        .map(filters ->
                            filters.getFilterByType(KindFilter.FILTER_KEY))
                        .flatMap(Collection::stream)
                        .map(KindFilter.class::cast)
                        .map(AbstractFilterable::getFilterable)
                        .findAny().orElseThrow(() ->
                            new InvalidReputationReqJsonException(reqMessage.getFiltersList(), KindFilter.FILTER_KEY)))) ?
            processReqKindService(reqMessage) :
            processReqKindTypeService(reqMessage));

    reqService.processIncoming(reqMessage1, sessionId);
  }

  private Filters processReqKindTypeService(ReqMessage reqMessage) {
    return reqKindTypeService.processIncoming(reqMessage.getFiltersList());
  }

  private Filters processReqKindService(ReqMessage reqMessage) {
    return reqKindService.processIncoming(reqMessage.getFiltersList());
  }

  private List<Filters> validateFiltersExist(List<Filters> filtersList) {
    filtersList.stream().findAny().orElseThrow(() -> new NostrException(Filters.FILTERS_CANNOT_BE_EMPTY));
    return filtersList;
  }
}
