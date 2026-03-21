package com.prosilion.afterimage.service.request;

import com.prosilion.afterimage.InvalidReputationReqJsonException;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.filter.AbstractFilterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.superconductor.base.service.request.ReqServiceIF;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
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
    log.debug("processIncoming(reqMessage, sessionId) [{}] with List<Filters>:\n  [{}]",
        sessionId,
        reqMessage.getFiltersList().stream()
            .map(filters -> filters.toString(2))
            .collect(Collectors.joining("],\n  [")));

    ReqMessage reqMessageAdaptedFilters = new ReqMessage(
        reqMessage.getSubscriptionId(),
        reqKindService.getKinds().stream()
            .anyMatch(kind ->
                kind.equals(
                    validateFiltersExist(reqMessage.getFiltersList()).stream()
                        .map(filters ->
                            filters.getFilterByType(KindFilter.FILTER_KEY))
                        .flatMap(Collection::stream)
                        .map(KindFilter.class::cast)
                        .map(AbstractFilterable::getFilterable)
                        .findAny().orElseThrow(() ->
                            new InvalidReputationReqJsonException(reqMessage.getFiltersList(), KindFilter.FILTER_KEY)))) ?
            processReqKindService(reqMessage) :
            processReqKindTypeService(reqMessage));

    reqService.processIncoming(reqMessageAdaptedFilters, sessionId);
  }

  private Filters processReqKindTypeService(ReqMessage reqMessage) {
    log.debug("processReqKindTypeService(reqMessage)...");
    return reqKindTypeService.processIncoming(reqMessage.getFiltersList());
  }

  private Filters processReqKindService(ReqMessage reqMessage) {
    log.debug("processReqKindService(reqMessage)...");
    return reqKindService.processIncoming(reqMessage.getFiltersList());
  }

  private List<Filters> validateFiltersExist(List<Filters> filtersList) {
    log.debug("validateFiltersExist(List<Filters> filtersList) called with List<filters>:\n  [{}]",
        filtersList.stream()
            .map(filters -> filters.toString(2))
            .collect(Collectors.joining("],\n  [")));
    filtersList.stream().findAny().orElseThrow(() -> new NostrException(Filters.FILTERS_CANNOT_BE_EMPTY));
    return filtersList;
  }
}
