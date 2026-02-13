package com.prosilion.afterimage.service.request;

import com.prosilion.afterimage.service.request.plugin.ReqKindTypePluginIF;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReqKindTypeService implements ReqKindTypeServiceIF {
  private final Map<Kind, Map<KindTypeIF, ReqKindTypePluginIF>> reqKindTypePluginMap;

  @Autowired
  public ReqKindTypeService(@NonNull List<ReqKindTypePluginIF> reqKindTypePlugins) {
    reqKindTypePluginMap = reqKindTypePlugins.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(ReqKindTypePluginIF::getKind, Collectors.toMap(
            ReqKindTypePluginIF::getKindType, Function.identity())));

    log.debug("Ctor (List<ReqKindTypePluginIF>) loaded values:\n{}",
        reqKindTypePlugins.stream()
            .sorted(Comparator.comparing(reqKindTypePluginIF ->
                reqKindTypePluginIF.getKind().getValue()))
            .map(reqKindTypePluginIF ->
                String.format("  Kind[%s]:%s -> KindType[%s]:%s -> %s",
                    reqKindTypePluginIF.getKind().getValue(),
                    reqKindTypePluginIF.getKind().getName().toUpperCase(),
                    reqKindTypePluginIF.getKindType().getKindDefinition().getValue(),
                    reqKindTypePluginIF.getKindType().getKindDefinition().getName().toUpperCase(),
                    reqKindTypePluginIF.getClass().getSimpleName()))
            .collect(Collectors.joining("\n")));
  }

  @Override
  public Filters processIncoming(@NonNull List<Filters> filtersList) throws NostrException {
    log.debug("ReqKindTypeService processIncoming(List<Filters>) with List<Filters>:\n{}",
        filtersList.stream()
            .map(filters -> filters.toString(2))
            .collect(Collectors.joining(",\n")));

    final Kind validatedReqKind = getReqKindPluginKind(filtersList, getKinds());
    log.debug("... with (1 of 4) validated reqKindTypePlugin kind:\n  {}",
        String.format("[%s]:%s",
            validatedReqKind.getValue(),
            validatedReqKind.getName().toUpperCase()));

    KindTypeIF validKindTypeIF = validatedReqKindType(validatedReqKind, getKindTypes());
    log.debug("... and (2 of 4) validated validKindTypeIF:\n  {}", validKindTypeIF);

    ReqKindTypePluginIF reqKindTypePluginIF = reqKindTypePluginMap
        .get(validatedReqKind)
        .get(validKindTypeIF);
    log.debug("... and (3 of 4) validated reqKindTypePluginIF KindTypeIF:\n  {}", reqKindTypePluginIF);

    log.debug("... which (4 of 4) maps to ReqKindTypePluginIF impl:\n  {}",
        String.format("[%s]:%s -> %s, class: %s",
            reqKindTypePluginIF.getKind().getValue(),
            reqKindTypePluginIF.getKind().getName(),
            String.format("%s",
                reqKindTypePluginIF.getKindType().getKind().getName()),
            reqKindTypePluginIF.getClass().getSimpleName()));

    Filters filters = reqKindTypePluginIF
        .processIncomingRequest(filtersList);

    return filters;
  }

  @Override
  public List<Kind> getKinds() {
    return reqKindTypePluginMap.keySet().stream().toList();
  }

  @Override
  public List<KindTypeIF> getKindTypes() {
    return reqKindTypePluginMap.values().stream()
        .map(Map::keySet)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
}
