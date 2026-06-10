package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.afterimage.InvalidReputationReqJsonException;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.ExternalIdentityTagFilter;
import com.prosilion.nostr.user.Identity;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ReqKindTypePlugin implements ReqKindTypePluginIF {
  @Getter
  private final Identity aImgIdentity;

  public ReqKindTypePlugin(@NonNull Identity aImgIdentity) {
    this.aImgIdentity = aImgIdentity;
  }

  public abstract List<String> includeReputationVariantFilters() throws NostrException;

  @Override
  final public Filters processIncomingRequest(@NonNull List<Filters> filtersList) throws NostrException {
    Stream<String> additionalFilters = includeReputationVariantFilters().stream();

    Stream<Filterable> filterables = additionalFilters.map(s ->
       matchFilterableKey(filtersList, s));
    Stream<Filterable> kindFilter = Stream.of(new KindFilter(getKind()));
    Stream<Filterable> filterable = Stream.of(matchFilterableKey(filtersList, ExternalIdentityTagFilter.FILTER_KEY));

    Stream<Filterable> concat = Stream.concat(
       Stream.concat(kindFilter, filterables)
       , filterable);
// TODO: revisit AddressTagFilter (repDefnCreator) inclusion/excluson
//   , matchFilterableKey(filtersList, AddressTagFilter.FILTER_KEY)

    return new Filters(concat.distinct().toList());
  }

  protected Filterable matchFilterableKey(List<Filters> filtersList, String key) {
    log.debug("matchFilterableKey: [{}]\nagainst List<Filters>:\n{}", key,
       filtersList.stream()
          .map(filters -> filters.toString(2))
          .collect(Collectors.joining(",\n")));

    Filterable filterable = filtersList.stream()
       .map(filters1 ->
          filters1.getFilterByType(key))
       .flatMap(Collection::stream)
       .findFirst().orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, key.concat(" tag")));
    log.debug("matched filterable: [{}]", filterable.getFilterKey());
    return filterable;
  }
}
