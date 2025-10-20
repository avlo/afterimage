package com.prosilion.afterimage.service.request;

import com.prosilion.afterimage.InvalidReputationReqJsonException;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
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

  public AfterimageReqService(
      @NonNull ReqServiceIF reqService,
      @NonNull ReqKindServiceIF reqKindService) {
    this.reqService = reqService;
    this.reqKindService = reqKindService;
  }

  @Override
  public void processIncoming(@NonNull ReqMessage reqMessage, @NonNull String sessionId) throws NostrException {
    reqService.processIncoming(
        new ReqMessage(
            reqMessage.getSubscriptionId(),
            reqKindService.processIncoming(
                reqMessage.getFiltersList())),
        sessionId);
  }

//  TODO: revisit kindMatches check, may not be necessary
//  private void kindMatches(ReqMessage reqMessage, List<Kind> kinds) {
//    Optional.of(kinds.stream().map(kind -> kindMatches(reqMessage, kind)))
//        .orElseThrow(() ->
//            new InvalidReputationReqJsonException(kinds, KindFilter.FILTER_KEY));
//  }
//  TODO: revisit kindMatches check, may not be necessary
  private boolean kindMatches(ReqMessage reqMessage, Kind kind) {
    return kind.equals(
        validateFiltersExist(reqMessage.getFiltersList()).stream()
            .map(filters ->
                filters.getFilterByType(KindFilter.FILTER_KEY))
            .flatMap(Collection::stream)
            .map(KindFilter.class::cast)
            .map(AbstractFilterable::getFilterable)
            .findAny().orElseThrow(() ->
                new InvalidReputationReqJsonException(reqMessage.getFiltersList(), KindFilter.FILTER_KEY)));
  }

  private List<Filters> validateFiltersExist(List<Filters> filtersList) {
    filtersList.stream().findAny().orElseThrow(() -> new NostrException(Filters.FILTERS_CANNOT_BE_EMPTY));
    return filtersList;
  }
}
