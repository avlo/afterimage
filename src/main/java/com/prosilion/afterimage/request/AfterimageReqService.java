package com.prosilion.afterimage.request;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.superconductor.service.request.ReqServiceIF;
import com.prosilion.superconductor.util.EmptyFiltersException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.NonNull;

public class AfterimageReqService implements ReqServiceIF {
  private final Map<Kind, ReqKindTypePlugin> reqKindTypePluginMap;
  private final ReqServiceIF reqService;

  public AfterimageReqService(
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
        new ReqMessage(sessionId, reqKindTypePluginMap.get(
                reqMessage.getFiltersList().stream()
                    .flatMap(filters ->
                        filters.getFilterByType(KindFilter.FILTER_KEY).stream())
                    .map(Filterable::getFilterable)
                    .map(Kind.class::cast)
                    .findFirst().orElseThrow(() ->
                        new EmptyFiltersException(
                            String.format("Valid Kind filter not specified, must be one of Kind [%s]", 
                                Strings.join(reqKindTypePluginMap.keySet(), ',')))))
            .processIncomingRequest(reqMessage)), sessionId);
  }
}
