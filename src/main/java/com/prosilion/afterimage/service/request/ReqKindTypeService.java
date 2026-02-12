package com.prosilion.afterimage.service.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosilion.afterimage.InvalidKindException;
import com.prosilion.afterimage.service.request.plugin.ReqKindTypePluginIF;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
  public ReqKindTypeService(@NonNull List<ReqKindTypePluginIF> reqKindTypePlugins) throws JsonProcessingException {
    reqKindTypePluginMap = reqKindTypePlugins.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(ReqKindTypePluginIF::getKind, Collectors.toMap(
            ReqKindTypePluginIF::getKindType, Function.identity())));
    log.debug("{} Ctor() loaded values:\n{}",
        getClass().getSimpleName(),
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(reqKindTypePluginMap));
  }

  @Override
  public Filters processIncoming(@NonNull List<Filters> filtersList) throws NostrException {
    log.debug("ReqKindTypeService processIncoming(List<Filters>) with List<Filters>:\n{}", 
        filtersList.stream()
            .map(Filters::toString)
            .collect(Collectors.joining(",\n")));

    final Kind kind = getReqKindPlugin(filtersList, reqKindTypePluginMap.keySet().stream().toList(), log);

    validateReferencedPubkeyTag(filtersList);

    return
        Optional
            .ofNullable(
                reqKindTypePluginMap.get(kind)).orElseThrow(() ->
                new InvalidKindException(kind.getName(), getKinds().stream().map(Kind::getName).toList()))
            .get(getKindTypes().stream().filter(kindTypeIF ->
                kindTypeIF.getName().equalsIgnoreCase(
                    validateExternalIdentityTag(
                        filtersList,
                        getKindTypes()))).findFirst().orElseThrow())
            .processIncomingRequest(filtersList);
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
