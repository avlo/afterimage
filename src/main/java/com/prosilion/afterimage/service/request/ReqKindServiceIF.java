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

  default Kind getReqKindPluginKind(List<Filters> filtersList, List<Kind> kinds) throws NostrException {
    Kind matchedKinds = filtersList.stream()
        .flatMap(filters ->
            filters.getFilterByType(KindFilter.FILTER_KEY).stream())
        .reduce(this::apply)
        .map(Filterable::getFilterable)
        .map(Kind.class::cast)
        .filter(kinds::contains)
        .orElseThrow(() ->
            new NostrException(
                String.format("Valid Kind filter not specified, must be one of Kind [%s]",
                    Strings.join(kinds, ','))));

    return matchedKinds;
  }
  private Filterable apply(Filterable filterable1, Filterable filterable2) {
    throw new NostrException(
        String.format("Multiple matches found for KindFilter [%s, %s]", filterable1.getFilterKey(), filterable2.getFilterKey()));
  }
}
