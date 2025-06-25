package com.prosilion.afterimage.request;

import com.prosilion.afterimage.util.InvalidReputationReqJsonException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.superconductor.service.request.ReqServiceIF;
import com.prosilion.superconductor.util.EmptyFiltersException;
import java.util.List;
import java.util.Optional;
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
    List<Kind> kinds = filtersList.stream()
        .flatMap(filters ->
            Optional.of(filters.getFilterByType(KindFilter.FILTER_KEY).stream())
                .orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, KindFilter.FILTER_KEY)))
        .map(KindFilter.class::cast)
        .map(KindFilter::getFilterable)
        .toList();

//    reqKindService.getKinds().stream()
//        .filter(kinds::contains)
//        .findFirst()
//        .ifPresentOrElse(kindType ->
//            reqKindService.processIncoming(filtersList)),
//        () -> reqKindTypeService.processIncoming(filtersList);

    if (reqKindService.getKinds().stream().anyMatch(kinds::contains)) {
      reqKindService.processIncoming(filtersList);
      return;
    }

    reqKindTypeService.processIncoming(filtersList);
  }
}
