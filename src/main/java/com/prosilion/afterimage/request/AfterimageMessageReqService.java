package com.prosilion.afterimage.request;

import com.prosilion.afterimage.util.InvalidReputationReqJsonException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.superconductor.service.message.req.ReqMessageServiceIF;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;

public class AfterimageMessageReqService implements ReqMessageServiceIF {
  public static final String ADDRESS_TAG_FILTER = AddressTagFilter.FILTER_KEY;
  private final Map<Kind, ReqKindTypePlugin> reqKindTypePluginMap;
  private final ReqMessageServiceIF reqMessageService;

  public AfterimageMessageReqService(
      @NonNull List<ReqKindTypePlugin> eventTypePlugins,
      @NonNull ReqMessageServiceIF reqMessageService) {
    this.reqMessageService = reqMessageService;
    this.reqKindTypePluginMap = eventTypePlugins.stream()
        .collect(
            Collectors.toMap(
                ReqKindTypePlugin::getKind,
                Function.identity()));
  }

  @Override
  public void processIncoming(@NonNull ReqMessage reqMessage, @NonNull String sessionId) {
    filterAddressTagsKind(reqMessage, sessionId)
        .ifPresentOrElse(
            correctKind ->
                reqMessageService.processIncoming(
                    new ReqMessage(
                        sessionId,
                        reqKindTypePluginMap.get(correctKind).processIncomingRequest(reqMessage)), sessionId),
            () -> processNoticeClientResponse(
                reqMessage,
                sessionId,
                String.format(InvalidReputationReqJsonException.INVALID_FILTERS, reqMessage.getFiltersList(), ADDRESS_TAG_FILTER)));
  }

  @Override
  public void processNoticeClientResponse(@NonNull ReqMessage reqMessage, @NonNull String sessionId, @NonNull String errorMessage) {
    reqMessageService.processNoticeClientResponse(reqMessage, sessionId, errorMessage);
  }

  private Optional<Kind> filterAddressTagsKind(ReqMessage reqMessage, String sessionId) {
    return reqMessage.getFiltersList().stream()
        .flatMap(filters ->
            filters.getFilterByType(ADDRESS_TAG_FILTER).stream())
        .map(Filterable::getFilterable)
        .map(AddressTag.class::cast)
        .map(AddressTag::getKind)
        .findFirst();
  }
}
