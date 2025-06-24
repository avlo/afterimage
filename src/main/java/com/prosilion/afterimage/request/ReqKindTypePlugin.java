package com.prosilion.afterimage.request;

import com.prosilion.afterimage.util.InvalidReputationReqJsonException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;

public abstract class ReqKindTypePlugin {
  private final String ADDRESS_TAG_FILTER = AddressTagFilter.FILTER_KEY;
  private final Identity aImgIdentity;

  public ReqKindTypePlugin(@NonNull Identity aImgIdentity) {
    this.aImgIdentity = aImgIdentity;
  }

  Filters processIncomingRequest(@NonNull ReqMessage reqMessage) {
    Filters filters = new Filters(
        validateRequiredFilterKindType(reqMessage.getFiltersList()).stream().map(publicKey ->
                new AddressTagFilter(
                    getAddressTag(
                        publicKey,
                        aImgIdentity)))
            .map(Filterable.class::cast)
            .toList());
    return filters;
  }

  private List<PublicKey> validateRequiredFilterKindType(List<Filters> filtersList) throws InvalidReputationReqJsonException {
    return filtersList.stream()
        .flatMap(filters ->
            Optional.of(filters.getFilterByType(ADDRESS_TAG_FILTER).stream())
                .orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, ADDRESS_TAG_FILTER)))
        .map(AddressTagFilter.class::cast)
        .map(AddressTagFilter::getFilterable)
        .filter(addressTag ->
            Optional.of(
                    addressTag.getKind().equals(getKind()))
//            note: below will throw exception if any filter among all filters does not contain 2113
//              may want to change/update this, revisit later
                .orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, getKind().getName())))
        .map(AddressTag::getPublicKey)
        .toList();
  }

  private AddressTag getAddressTag(PublicKey publicKey, Identity aImgIdentity) {
    return new AddressTag(
        getKind(),
        publicKey,
        new IdentifierTag(
//                       TODO: below identiy needs design/thought
            aImgIdentity.getPublicKey().toHexString())
    );
  }

  public abstract Kind getKind();

  public abstract KindTypeIF getKindType();
}
