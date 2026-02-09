package com.prosilion.afterimage.service.request;

import com.prosilion.afterimage.InvalidKindException;
import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.filter.tag.ExternalIdentityTagFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import com.prosilion.superconductor.base.util.EmptyFiltersException;
import java.util.List;
import java.util.Optional;

// TODO: refactor when testing complete
public interface ReqKindTypeServiceIF extends ReqKindServiceIF {
  List<KindTypeIF> getKindTypes();

  default String validateExternalIdentityTag(List<Filters> userProvidedKindTypes, List<KindTypeIF> acceptableKindTypes) throws NostrException {
    List<String> acceptableKindTypeStrings = acceptableKindTypes.stream().map(KindTypeIF::getName).map(String::toUpperCase).toList();

    ExternalIdentityTag userProvidedExternalIdentityTag = userProvidedKindTypes.stream()
        .flatMap(filters ->
            filters.getFilterByType(ExternalIdentityTagFilter.FILTER_KEY).stream())
        .findFirst().orElseThrow(() ->
            new EmptyFiltersException(
                userProvidedKindTypes, "ExternalIdentityTag")).getFilterable();

    String userProvidedExternalIdentity = Optional.ofNullable(userProvidedExternalIdentityTag.getIdentity())
        .map(String::toUpperCase).orElseThrow(() ->
            new InvalidTagException(userProvidedExternalIdentityTag.getIdentity(), acceptableKindTypeStrings));

    if (!acceptableKindTypeStrings.contains(userProvidedExternalIdentity)) {
      throw new InvalidKindException(userProvidedExternalIdentity, acceptableKindTypeStrings);
    }

    return userProvidedExternalIdentity;
  }

  @Deprecated(since = "validateIdentifierTag is non-sequitur")
  default String validateIdentifierTag(List<Filters> userProvidedKindTypes, List<KindTypeIF> acceptableKindTypes) throws NostrException {
    List<String> acceptableKindTypeStrings = acceptableKindTypes.stream().map(KindTypeIF::getName).map(String::toUpperCase).toList();

    IdentifierTag userProvidedIdentifierTag = userProvidedKindTypes.stream()
        .flatMap(filters ->
            filters.getFilterByType(IdentifierTagFilter.FILTER_KEY).stream())
        .findFirst().orElseThrow(() ->
            new EmptyFiltersException(
                userProvidedKindTypes, "IdentifierTag")).getFilterable();

    String userProvidedUuid = Optional.ofNullable(userProvidedIdentifierTag.getUuid()).orElseThrow(() ->
        new InvalidTagException(userProvidedIdentifierTag.getUuid(), acceptableKindTypeStrings));

    if (!acceptableKindTypeStrings.contains(userProvidedUuid)) {
      throw new InvalidKindException(userProvidedUuid, acceptableKindTypeStrings);
    }

    return userProvidedUuid;
  }

  @Deprecated(since = "validateAddressTag is non-sequitur")
  default String validateAddressTag(List<Filters> userProvidedKindTypes, List<KindTypeIF> acceptableKindTypes) throws NostrException {
// TODO: refactor when testing complete
    List<String> acceptableKindTypeStrings = acceptableKindTypes.stream().map(KindTypeIF::getName).map(String::toUpperCase).toList();

    Filterable filterable = userProvidedKindTypes.stream()
        .flatMap(filters ->
            filters.getFilterByType(AddressTagFilter.FILTER_KEY).stream())
        .findFirst().orElseThrow(() ->
            new EmptyFiltersException(
                userProvidedKindTypes, "AddressTagFilter"));

    AddressTag userProvidedAddressTag = filterable.getFilterable();

    String userProvidedUuid = Optional.ofNullable(userProvidedAddressTag.getIdentifierTag()).orElseThrow(() ->
            new InvalidTagException("AddressTag is missing an IdentifierTag UUID", acceptableKindTypeStrings))
        .getUuid();

    if (!acceptableKindTypeStrings.contains(userProvidedUuid)) {
      throw new InvalidKindException(userProvidedUuid, acceptableKindTypeStrings);
    }

    return userProvidedUuid;
  }

  default void validateReferencedPubkeyTag(List<Filters> userProvidedKindTypes) throws NostrException {
    userProvidedKindTypes.stream()
        .flatMap(filters ->
            filters.getFilterByType(ReferencedPublicKeyFilter.FILTER_KEY).stream())
        .findFirst().orElseThrow(() ->
            new EmptyFiltersException(
                userProvidedKindTypes, "PubKeyTag"));
  }
}
