package com.prosilion.afterimage.service.request;

import com.prosilion.afterimage.InvalidKindException;
import com.prosilion.afterimage.service.request.plugin.ReqKindTypePluginIF;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.filter.Filters;
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
  private final Map<Kind, Map<KindTypeIF, ReqKindTypePluginIF>> reqKindTypePluginMap;

  @Autowired
  public ReqKindTypeService(@NonNull List<ReqKindTypePluginIF> reqKindTypePlugins) {
    reqKindTypePluginMap = reqKindTypePlugins.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(ReqKindTypePluginIF::getKind, Collectors.toMap(
            ReqKindTypePluginIF::getKindType, Function.identity())));
  }

  @Override
  public Filters processIncoming(@NonNull List<Filters> filtersList) throws NostrException {
    final Kind kind = getReqKindPlugin(filtersList, reqKindTypePluginMap.keySet().stream().toList());
    validateReferencedPubkeyTag(filtersList);

    return
        Optional
            .ofNullable(
                reqKindTypePluginMap.get(kind)).orElseThrow(() ->
                new InvalidKindException(kind.getName(), getKinds().stream().map(Kind::getName).toList()))
            .get(getKindTypes().stream().filter(kindTypeIF ->
                kindTypeIF.getName().equalsIgnoreCase(
                    validateIdentifierTag(
                        filtersList,
                        getKindTypes()))).findFirst().orElseThrow())
            .processIncomingRequest(filtersList);
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
