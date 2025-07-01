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
  private final Map<Kind, Map<KindTypeIF, ReqKindTypePluginIF<KindTypeIF>>> reqKindTypePluginMap;

  @Autowired
  public ReqKindTypeService(@NonNull List<ReqKindTypePluginIF<KindTypeIF>> reqKindTypePlugins) {
    reqKindTypePluginMap = reqKindTypePlugins.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(ReqKindTypePluginIF<KindTypeIF>::getKind, Collectors.toMap(
            ReqKindTypePluginIF<KindTypeIF>::getKindType, Function.identity())));
  }

  @Override
  public Filters processIncoming(@NonNull List<Filters> filtersList) throws NostrException {
// TODO: refactor when testing complete    
    List<Kind> list = reqKindTypePluginMap.keySet().stream().toList();
    Kind kind = getReqKindPlugin(filtersList, list);

    Map<KindTypeIF, ReqKindTypePluginIF<KindTypeIF>> value = Optional.ofNullable(
        reqKindTypePluginMap.get(kind)).orElseThrow(() ->
        new InvalidKindException(kind.getName(), getKinds().stream().map(Kind::getName).toList()));

    validateReferencedPubkeyTag(filtersList);

    String uuid = validateIdentifierTag(filtersList, getKindTypes());

    KindTypeIF reqKindTypePlugin = getKindTypes().stream().filter(k -> k.getName().equalsIgnoreCase(uuid)).findFirst().orElseThrow();

    Filters filters = value.get(reqKindTypePlugin).processIncomingRequest(filtersList);
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
