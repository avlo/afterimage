package com.prosilion.afterimage.relay;

import com.prosilion.afterimage.InvalidReputationReqJsonException;
import com.prosilion.afterimage.request.ReqKindServiceIF;
import com.prosilion.afterimage.request.ReqKindTypeServiceIF;
import com.prosilion.nostr.filter.AbstractFilterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.superconductor.service.request.ReqServiceIF;
import com.prosilion.superconductor.util.EmptyFiltersException;
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
  public void processIncoming(@NonNull ReqMessage reqMessage, @NonNull String sessionId) throws EmptyFiltersException {

    reqService.processIncoming(
        new ReqMessage(
            reqMessage.getSubscriptionId(),
            reqKindService.getKinds().stream()
                .anyMatch(kind ->
                    kind.equals(
                        validateFiltersExist(
                            reqMessage.getFiltersList()
                        ).stream()
                            .map(filters ->
                                filters.getFilterByType(KindFilter.FILTER_KEY))
                            .flatMap(Collection::stream)
                            .map(KindFilter.class::cast)
                            .map(AbstractFilterable::getFilterable)
                            .findAny().orElseThrow(() ->
                                new InvalidReputationReqJsonException(reqMessage.getFiltersList(), KindFilter.FILTER_KEY)))) ?
                reqKindService.processIncoming(reqMessage.getFiltersList()) :
                reqKindTypeService.processIncoming(reqMessage.getFiltersList())), sessionId);
  }

  private List<Filters> validateFiltersExist(List<Filters> filtersList) {
    filtersList.stream().findAny().orElseThrow(() -> new EmptyFiltersException(Filters.FILTERS_CANNOT_BE_EMPTY));
    return filtersList;
  }
}
