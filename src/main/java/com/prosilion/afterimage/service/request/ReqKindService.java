package com.prosilion.afterimage.service.request;

import com.prosilion.afterimage.service.request.plugin.ReqKindPluginIF;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import java.util.ArrayList;
import java.util.Comparator;
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

    log.debug("Ctor (List<ReqKindPluginIF>) loaded values:\n{}",
        reqKindPlugins.stream()
            .sorted(Comparator.comparing(reqKindPluginIF -> 
                reqKindPluginIF.getKind().getValue()))
            .map(reqKindPluginIF ->
                String.format("  Kind[%s]:%s -> %s",
                    reqKindPluginIF.getKind().getValue(),
                    reqKindPluginIF.getKind().getName().toUpperCase(),
                    reqKindPluginIF.getClass().getSimpleName()))
            .collect(Collectors.joining("\n")));
  }

  @Override
  public Filters processIncoming(@NonNull List<Filters> filtersList) throws NostrException {
    log.debug("ReqKindService processIncoming(List<Filters>) with List<Filters>:\n{}",
        filtersList.stream()
            .map(filters -> filters.toString(2))
            .collect(Collectors.joining(",\n")));

    Kind reqKindPlugin = getReqKindPluginKind(
        filtersList,
        reqKindPluginsMap.keySet().stream().toList());

    log.debug("processIncoming(List<Filters>) using reqKindPlugin kind:\n  {}",
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
