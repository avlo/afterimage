package com.prosilion.afterimage.service.request;

import com.prosilion.afterimage.service.request.plugin.ReqKindPluginIF;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.superconductor.util.EmptyFiltersException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class ReqKindService implements ReqKindServiceIF {
  private final Map<Kind, ReqKindPluginIF<Kind>> reqKindTypePluginMap;

  @Autowired
  public ReqKindService(@NonNull List<ReqKindPluginIF<Kind>> reqKindPlugins) {
    this.reqKindTypePluginMap = reqKindPlugins.stream()
        .collect(
            Collectors.toMap(
                ReqKindPluginIF::getKind,
                Function.identity()));
  }

  @Override
  public Filters processIncoming(@NonNull List<Filters> filtersList) throws EmptyFiltersException {
    List<Kind> list = reqKindTypePluginMap.keySet().stream().toList();
    Kind reqKindTypePlugin = getReqKindPlugin(filtersList, list);
    return reqKindTypePluginMap.get(reqKindTypePlugin).processIncomingRequest(filtersList);
  }

  @Override
  public List<Kind> getKinds() {
    return new ArrayList<>(reqKindTypePluginMap.keySet());
  }
}
