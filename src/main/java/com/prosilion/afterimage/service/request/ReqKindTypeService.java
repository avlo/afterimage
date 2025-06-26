package com.prosilion.afterimage.service.request;

import com.prosilion.afterimage.service.request.plugin.ReqKindTypePlugin;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.superconductor.util.EmptyFiltersException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class ReqKindTypeService implements ReqKindTypeServiceIF {
  private final Map<Kind, Map<KindTypeIF, ReqKindTypePlugin>> reqKindTypePluginMap;

  @Autowired
  public ReqKindTypeService(@NonNull List<ReqKindTypePlugin> reqKindTypePlugins) {
    reqKindTypePluginMap = reqKindTypePlugins.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(ReqKindTypePlugin::getKind, Collectors.toMap(
            ReqKindTypePlugin::getKindType, Function.identity())));
  }

  @Override
  public Filters processIncoming(@NonNull List<Filters> filtersList) throws EmptyFiltersException {
    List<Kind> list = reqKindTypePluginMap.keySet().stream().toList();
    Kind kind = getReqKindPlugin(filtersList, list);

    Map<KindTypeIF, ReqKindTypePlugin> value = Optional.ofNullable(
        reqKindTypePluginMap.get(kind)).orElseThrow();

    List<KindTypeIF> definedKindTypes = filtersList.stream()
        .map(filters ->
            filters.getFilterByType(IdentifierTagFilter.FILTER_KEY))
        .map(KindTypeIF.class::cast).toList();

    ReqKindTypePlugin reqKindTypePlugin = definedKindTypes.stream().map(value::get).findFirst().orElseThrow();
    Filters filters = reqKindTypePlugin.processIncomingRequest(filtersList);
    return filters;
  }

  @Override
  public List<Kind> getKinds() {
    List<Kind> list = reqKindTypePluginMap.keySet().stream().toList();
    return list;
  }

  @Override
  public List<KindTypeIF> getKindTypes() {
    List<KindTypeIF> list = reqKindTypePluginMap.values().stream()
        .map(Map::keySet)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    return list;
  }
}
