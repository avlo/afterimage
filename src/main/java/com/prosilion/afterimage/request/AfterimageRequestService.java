package com.prosilion.afterimage.request;

import com.prosilion.afterimage.util.InvalidReputationReqJsonException;
import com.prosilion.superconductor.service.request.ReqServiceIF;
import com.prosilion.superconductor.util.EmptyFiltersException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import nostr.event.Kind;
import nostr.event.filter.AddressTagFilter;
import nostr.event.filter.Filterable;
import nostr.event.impl.GenericEvent;
import nostr.event.message.ReqMessage;
import nostr.event.tag.AddressTag;

public class AfterimageRequestService<T extends GenericEvent, U extends Kind> implements ReqServiceIF<T> {
  public static final String ADDRESS_TAG_FILTER = AddressTagFilter.FILTER_KEY;
  private final Map<Kind, ReqKindTypePlugin<U>> reqKindTypePluginMap;
  private final ReqServiceIF<T> reqService;

  public AfterimageRequestService(
      @NonNull List<ReqKindTypePlugin<U>> eventTypePlugins,
      @NonNull ReqServiceIF<T> reqService) {
    this.reqService = reqService;
    this.reqKindTypePluginMap = eventTypePlugins.stream()
        .collect(
            Collectors.toMap(
                ReqKindTypePlugin::getKind,
                Function.identity()));
  }

  @Override
  public <V extends ReqMessage> void processIncoming(@NonNull V reqMessage, @NonNull String sessionId) throws EmptyFiltersException {
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
        .map(Kind::valueOf).toList();

    if (addressKinds.size() != 1) throw new InvalidReputationReqJsonException(reqMessage, ADDRESS_TAG_FILTER);

    return addressKinds.getFirst();
  }
}
