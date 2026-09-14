package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CuratedFormulaEventRequestPlugin implements ReqKindPluginIF { // kind 30_006
  @Override
  public Filters processIncomingRequest(@NonNull List<Filters> filtersList) {
    log.debug("processIncomingRequest with List<Filters>:\n  [{}]",
        filtersList.stream()
            .map(filters -> filters.toString(2))
            .collect(Collectors.joining("],\n [")));

    Filters suspectOverridenFilters = new Filters(new KindFilter(getKind()));
    log.debug("suspectOverridenFilters Filters:\n  [{}]", suspectOverridenFilters);

    List<Filters> concattedFilters = Stream.concat(
        filtersList.stream(),
        Stream.of(suspectOverridenFilters)).distinct().toList();

    log.debug("concatted filtersList.add(suspectOverridenFilters) List<Filters>:\n  [{}]",
        concattedFilters.stream()
            .map(filters -> filters.toString(2))
            .collect(Collectors.joining("],\n [")));

    return suspectOverridenFilters;
  }

  @Override
  public Kind getKind() {
    return Kind.CURATION_SETS_FORMULA_EVENT; // kind 30_006
  }
}
