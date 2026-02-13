package com.prosilion.afterimage.service.request;

import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.springframework.lang.NonNull;

public interface ReqKindServiceIF {
  Filters processIncoming(@NonNull List<Filters> filters) throws NostrException;

  List<Kind> getKinds();

  default Kind getReqKindPlugin(List<Filters> filtersList, List<Kind> kinds, Logger log) throws NostrException {
    log.debug("ReqKindServiceIF impl class {} processIncoming(List<Filters>) with List<Filters>:\n{}\nfiltered by kinds:\n  {}",
        getClass().getSimpleName(),
        filtersList.stream()
            .map(filters -> filters.toString(2))
            .collect(Collectors.joining(",\n")),
        kinds.stream()
            .map(kind ->
                String.format("  %-3s:%s",
                    kind.getValue(),
                    kind.getName()))
            .collect(Collectors.joining("\n")));

    Kind reqKindPlugin = getReqKindPlugin(filtersList, kinds);
    log.debug("returning first reqKindPlugin kind matched:\n  {} : {}", reqKindPlugin.getValue(), reqKindPlugin.getName().toUpperCase());
    return reqKindPlugin;
  }

  default Kind getReqKindPlugin(List<Filters> filtersList, List<Kind> kinds) throws NostrException {
    Kind kind = filtersList.stream()
        .flatMap(filters ->
            filters.getFilterByType(KindFilter.FILTER_KEY).stream())
        .map(Filterable::getFilterable)
        .map(Kind.class::cast)
        .findFirst().orElseThrow(() ->
            new NostrException(
                String.format("Valid Kind filter not specified, must be one of Kind [%s]",
                    Strings.join(kinds, ','))));
    return kind;
  }
}
