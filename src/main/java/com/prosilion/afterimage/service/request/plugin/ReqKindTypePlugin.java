package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.afterimage.InvalidReputationReqJsonException;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.user.Identity;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ReqKindTypePlugin implements ReqKindTypePluginIF {
  public static final String REF_PUBKEY_FILTER_KEY = ReferencedPublicKeyFilter.FILTER_KEY;

  @Getter
  private final Identity aImgIdentity;

  public ReqKindTypePlugin(@NonNull Identity aImgIdentity) {
    this.aImgIdentity = aImgIdentity;
  }

  protected ReferencedPublicKeyFilter getReferencedPublicKeyFilter(List<Filters> filtersList) {
    log.debug("processIncoming(List<Filters>)\n  with List<Filters>:\n{}",
       filtersList.stream()
          .map(filters -> filters.toString(2))
          .collect(Collectors.joining(",\n")));

    List<Filterable> filterableStream = filtersList.stream()
       .map(filters1 ->
          filters1.getFilterByType(REF_PUBKEY_FILTER_KEY))
       .flatMap(Collection::stream).toList();

    log.debug("contains req'd ReferencedPublicKeyFilter.FILTER_KEY? [ {} ]", !filterableStream.isEmpty() ?
       "TRUE(ReputationRequestPlugin)" : "FALSE(ReputationRequestPlugin)");

    return filterableStream.stream()
       .map(ReferencedPublicKeyFilter.class::cast)
       .findAny().orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, REF_PUBKEY_FILTER_KEY.concat(" tag")));
  }

  protected AddressTagFilter getAddressTagFilter(List<Filters> filtersList) {
    return filtersList.stream()
       .map(filters ->
          filters.getFilterByType(AddressTagFilter.FILTER_KEY))
       .flatMap(Collection::stream)
       .map(AddressTagFilter.class::cast)
       .findAny().orElseThrow(() -> new InvalidReputationReqJsonException(filtersList,
          AddressTagFilter.FILTER_KEY.concat(" tag")));
  }
}
