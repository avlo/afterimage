package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AfterimageFollowSetsRequestPlugin implements ReqKindPluginIF { // kind 30_000
  public AfterimageFollowSetsRequestPlugin() {
    log.debug("loaded {} bean", getClass().getSimpleName());
  }

  @Override
  public Filters processIncomingRequest(@NonNull List<Filters> filtersList) {
    System.out.println("0000000000000000000000");
    System.out.println("0000000000000000000000");
    log.debug("{} processIncomingRequest with List<Filters>:\n{}", getClass().getSimpleName(),
        filtersList.stream()
            .map(filters -> filters.toString(2))
            .collect(Collectors.joining(",\n")));

    System.out.println(" ------- ");

    Filters suspectOverridenFilters = new Filters(new KindFilter(getKind()));
    log.debug("suspectOverridenFilters Filters:\n{}",
        suspectOverridenFilters.toString());

    System.out.println(" ------- ");

    filtersList.add(suspectOverridenFilters);
    log.debug("{} concatted filtersList.add(suspectOverridenFilters) List<Filters>:\n{}", getClass().getSimpleName(),
        filtersList.stream()
            .map(filters -> filters.toString(2))
            .collect(Collectors.joining("\n")));

    System.out.println("0000000000000000000000");
    System.out.println("0000000000000000000000");
    System.out.println("returning suspectOverridenFilters Filters");
    return suspectOverridenFilters;
  }

  @Override
  public Kind getKind() {
    return Kind.FOLLOW_SETS; // kind 30_000
  }
}
