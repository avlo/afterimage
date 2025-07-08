package com.prosilion.afterimage.service.request;

import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import java.util.List;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.NonNull;

public interface ReqKindServiceIF {
  Filters processIncoming(@NonNull List<Filters> filters) throws NostrException;

  List<Kind> getKinds();

  default Kind getReqKindPlugin(List<Filters> filtersList, List<Kind> kinds) throws NostrException {
    return filtersList.stream()
        .flatMap(filters ->
            filters.getFilterByType(KindFilter.FILTER_KEY).stream())
        .map(Filterable::getFilterable)
        .map(Kind.class::cast)
        .findFirst().orElseThrow(() ->
            new NostrException(
                String.format("Valid Kind filter not specified, must be one of Kind [%s]",
                    Strings.join(kinds, ','))));
  }
}
