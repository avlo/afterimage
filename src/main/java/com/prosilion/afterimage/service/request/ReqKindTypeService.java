package com.prosilion.afterimage.service.request;

import com.prosilion.afterimage.service.request.plugin.ReqKindTypePluginIF;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import java.util.Collection;
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

    String validatedExternalIdentityTag = validateExternalIdentityTag(
        filtersList,
        getKindTypes());

    final Kind reqKindPlugin = getReqKindPlugin(
        filtersList,
        reqKindTypePluginMap.keySet().stream().toList());

    log.debug("... using reqKindPlugin kind:\n  {}",
        String.format("%s:%s",
            reqKindPlugin.getName().toUpperCase(),
            reqKindPlugin.getValue()));

    ReqKindTypePluginIF reqKindTypePluginIF =
        reqKindTypePluginMap.get(reqKindPlugin)
            .get(getKindTypes().stream().filter(kindTypeIF ->
                kindTypeIF.getName().equalsIgnoreCase(
                    validatedExternalIdentityTag)).findFirst().orElseThrow());
    log.debug("which maps to ReqKindTypePluginIF impl:\n  {}",
        reqKindTypePluginIF.getClass().getSimpleName());

    Filters filters = reqKindTypePluginIF
        .processIncomingRequest(filtersList);

    return filters;
  }

  @Override
  public List<Kind> getKinds() {
//    TODO: check 30009 BADGE_DEFINITION_REPUTATION_EVENT vs 30009 BADGE_DEFINITION_AWARD(UP/DOWNVOTE)_EVENT  
    return reqKindTypePluginMap.keySet().stream().toList();
  }

  @Override
  public List<KindTypeIF> getKindTypes() {
//    TODO: check 30009 BADGE_DEFINITION_REPUTATION_EVENT vs 30009 BADGE_DEFINITION_AWARD(UP/DOWNVOTE)_EVENT    
    return reqKindTypePluginMap.values().stream()
        .map(Map::keySet)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
}
