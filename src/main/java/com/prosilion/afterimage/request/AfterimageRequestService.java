package com.prosilion.afterimage.request;

import com.prosilion.afterimage.util.InvalidReputationReqJsonException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.superconductor.service.request.ReqServiceIF;
import com.prosilion.superconductor.util.EmptyFiltersException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;

public class AfterimageRequestService implements ReqServiceIF {
  public static final String ADDRESS_TAG_FILTER = AddressTagFilter.FILTER_KEY;
  private final Map<Kind, ReqKindTypePlugin> reqKindTypePluginMap;
  private final ReqServiceIF reqService;

  public AfterimageRequestService(
      @NonNull List<ReqKindTypePlugin> eventTypePlugins,
      @NonNull ReqServiceIF reqService) {
    this.reqService = reqService;
    this.reqKindTypePluginMap = eventTypePlugins.stream()
        .collect(
            Collectors.toMap(
                ReqKindTypePlugin::getKind,
                Function.identity()));
  }

  @Override
  public void processIncoming(@NonNull ReqMessage reqMessage, @NonNull String sessionId) throws EmptyFiltersException {
    reqService.processIncoming(
        new ReqMessage(
            sessionId,
            reqKindTypePluginMap.get(
                    filterAddressTagsKind(reqMessage))
                .processIncomingRequest(reqMessage)),
        sessionId);
  }

  private Kind filterAddressTagsKind(ReqMessage reqMessage) {
    List<Kind> addressKinds = reqMessage.getFiltersList().stream()
        .flatMap(filters ->
            filters.getFilterByType(ADDRESS_TAG_FILTER).stream())
        .map(Filterable::getFilterable)
        .map(AddressTag.class::cast)
        .map(AddressTag::getKind)
        .toList();

    if (addressKinds.size() != 1) throw new InvalidReputationReqJsonException(reqMessage, ADDRESS_TAG_FILTER);

    return addressKinds.getFirst();
  }
}
