package com.prosilion.afterimage.service.request;

import com.prosilion.afterimage.service.request.plugin.ReqKindPluginIF;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReqKindService implements ReqKindServiceIF {
  private final Map<Kind, ReqKindPluginIF> reqKindPluginsMap;

  @Autowired
  public ReqKindService(@NonNull List<ReqKindPluginIF> reqKindPlugins) {
    this.reqKindPluginsMap = reqKindPlugins.stream()
        .collect(
            Collectors.toMap(
                ReqKindPluginIF::getKind,
                Function.identity()));

    log.debug("{} ctor (List<ReqKindPluginIF>) with values:\n{}", getClass().getSimpleName(),
        reqKindPlugins.stream()
            .map(reqKindPluginIF ->
                String.format("  %s:%s -> %s",
                    reqKindPluginIF.getKind().getValue(),
                    reqKindPluginIF.getKind().getName(),
                    reqKindPluginIF.getClass().getSimpleName()))
            .collect(Collectors.joining("\n")));
  }

  @Override
  public Filters processIncoming(@NonNull List<Filters> filtersList) throws NostrException {
    Kind reqKindPlugin = getReqKindPlugin(
        filtersList,
        reqKindPluginsMap.keySet().stream().toList());

    log.debug("{} processIncoming(List<Filters>) using reqKindPlugin kind:\n  {}",
        getClass().getSimpleName(),
        String.format("%s:%s",
            reqKindPlugin.getValue(),
            reqKindPlugin.getName()));

    ReqKindPluginIF reqKindPluginIF = reqKindPluginsMap.get(reqKindPlugin);
    log.debug("which maps to ReqKindPluginIF impl:\n  {}",
        reqKindPluginIF.getClass().getSimpleName());

    Filters filters = reqKindPluginIF.processIncomingRequest(filtersList);

    log.debug("which returned Filters:\n{}",
        filters.toString());

    return filters;
  }

  @Override
  public List<Kind> getKinds() {
    return new ArrayList<>(reqKindPluginsMap.keySet());
  }
}
