package com.prosilion.afterimage.request;

import com.prosilion.afterimage.util.InvalidReputationReqJsonException;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import nostr.base.PublicKey;
import nostr.event.Kind;
import nostr.event.filter.AddressTagFilter;
import nostr.event.filter.Filterable;
import nostr.event.filter.Filters;
import nostr.event.message.ReqMessage;
import nostr.event.tag.AddressTag;
import nostr.event.tag.IdentifierTag;
import nostr.id.Identity;

public abstract class ReqKindTypePlugin<T extends Kind> {
  private final String ADDRESS_TAG_FILTER = AddressTagFilter.FILTER_KEY;
  private final Identity aImgIdentity;

  public ReqKindTypePlugin(@NonNull Identity aImgIdentity) {
    this.aImgIdentity = aImgIdentity;
  }

  <V extends ReqMessage> Filters processIncomingRequest(@NonNull V reqMessage) {
    Filters filters = new Filters(
        validateRequiredFilterByAddressTag(reqMessage).stream().map(publicKey ->
                new AddressTagFilter<>(getAddressTag(publicKey, aImgIdentity)))
            .map(Filterable.class::cast)
            .toList());
    return filters;
  }

  private <U extends ReqMessage> List<PublicKey> validateRequiredFilterByAddressTag(U reqMessage) throws InvalidReputationReqJsonException {
    return reqMessage.getFiltersList().stream()
        .flatMap(filters ->
            Optional.of(filters.getFilterByType(ADDRESS_TAG_FILTER).stream())
                .orElseThrow(() -> new InvalidReputationReqJsonException(reqMessage, ADDRESS_TAG_FILTER)))
        .map(AddressTagFilter.class::cast)
        .map(AddressTagFilter::getFilterable)
        .map(AddressTag.class::cast)
        .filter(addressTag ->
            Optional.of(
                    addressTag.getKind().equals(getKind().getValue()))
//            note: below will throw exception if any filter among all filters does not contain 2113
//              may want to change/update this, revisit later
                .orElseThrow(() -> new InvalidReputationReqJsonException(reqMessage, getKind().getName())))
        .map(AddressTag::getPublicKey)
        .toList();
  }

  private AddressTag getAddressTag(PublicKey publicKey, Identity aImgIdentity) {
    return new AddressTag(
        getKind().getValue(),
        publicKey,
        new IdentifierTag(
//                       TODO: below identiy needs design/thought
            aImgIdentity.getPublicKey().toHexString())
    );
  }

  abstract T getKind();
}
