package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ReqKindPlugin(Kind getKind) implements ReqKindPluginIF {

  public Filters processIncomingRequest(@NonNull List<Filters> filtersList) {
    log.debug("processIncoming AbstractBadgeAwardEventRequest with List<Filters>:\n  [{}]",
       filtersList.stream()
          .map(filters -> filters.toString(2))
          .collect(Collectors.joining("],\n [")));

    Filters filters = filtersList.getFirst();
    log.debug("filters Filters:\n  [{}]", filters);

    return filters;
  }
}
