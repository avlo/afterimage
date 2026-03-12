package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AfterimageFollowSetsRequestPlugin implements ReqKindPluginIF { // kind 30_000
  @Override
  public Filters processIncomingRequest(@NonNull List<Filters> filtersList) {
    log.debug("processIncomingRequest with List<Filters>:\n{}",
        filtersList.stream()
            .map(filters -> filters.toString(2))
            .collect(Collectors.joining(",\n")));

    Filters suspectOverridenFilters = new Filters(new KindFilter(getKind()));
    log.debug("suspectOverridenFilters Filters:\n{}",
        suspectOverridenFilters.toString());

    List<Filters> concattedFilters = Stream.concat(
        filtersList.stream(),
        Stream.of(suspectOverridenFilters)).distinct().toList();

    log.debug("concatted filtersList.add(suspectOverridenFilters) List<Filters>:\n{}",
        concattedFilters.stream()
            .map(filters -> filters.toString(2))
            .collect(Collectors.joining("\n")));

    return suspectOverridenFilters;
  }

  @Override
  public Kind getKind() {
    return Kind.FOLLOW_SETS; // kind 30_000
  }
}
