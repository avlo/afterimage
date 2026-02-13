package com.prosilion.afterimage.service.request;

import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.filter.tag.ExternalIdentityTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import com.prosilion.superconductor.base.util.EmptyFiltersException;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

// TODO: refactor when testing complete
public interface ReqKindTypeServiceIF extends ReqKindServiceIF {
  List<KindTypeIF> getKindTypes();

  default KindTypeIF validatedExternalIdentityTag(List<Filters> userProvidedKindTypes, List<KindTypeIF> acceptableKindTypes) throws NostrException {
    ExternalIdentityTag userProvidedExternalIdentityTag = userProvidedKindTypes.stream()
        .flatMap(filters ->
            filters.getFilterByType(ExternalIdentityTagFilter.FILTER_KEY).stream())
        .reduce(this::apply)
        .orElseThrow(() ->
            new EmptyFiltersException(
                userProvidedKindTypes, "ExternalIdentityTag")).getFilterable();

    KindTypeIF kindTypeIF1 = acceptableKindTypes.stream()
        .filter(kindTypeIF ->
            kindTypeIF.getName().equals(userProvidedExternalIdentityTag.getIdentity()))
        .reduce(this::apply)
        .orElseThrow(() ->
            new NostrException(
                String.format("Valid KindTypeIf filter not specified, must be single exclusive of KindTypeIF [%s]",
                    Strings.join(acceptableKindTypes, ','))));

    return kindTypeIF1;
  }

  default KindTypeIF validatedReqKindType(Kind userProvidedKind, List<KindTypeIF> acceptableKindTypes) throws NostrException {
    KindTypeIF matchedKindType = acceptableKindTypes.stream()
        .filter(kindTypeIF ->
            kindTypeIF.getKind().equals(userProvidedKind))
        .reduce(this::apply)
        .orElseThrow(() ->
            new NostrException(
                String.format("Valid KindTypeIf filter not specified, must be single exclusive of KindTypeIF [%s]",
                    Strings.join(acceptableKindTypes, ','))));

    return matchedKindType;
  }

  @Deprecated(since = "validateAddressTag is non-sequitur")
  default KindTypeIF validateAddressTag(List<Filters> userProvidedKindTypes, List<KindTypeIF> acceptableKindTypes) throws NostrException {
    AddressTag userProvidedAddressTag = userProvidedKindTypes.stream()
        .flatMap(filters ->
            filters.getFilterByType(AddressTagFilter.FILTER_KEY).stream())
        .reduce(this::apply)
        .orElseThrow(() ->
            new EmptyFiltersException(
                userProvidedKindTypes, "AddressTag")).getFilterable();

    KindTypeIF matchedKindType = acceptableKindTypes.stream()
        .filter(kindTypeIF ->
            kindTypeIF.getKind().equals(userProvidedAddressTag.getKind()))
        .reduce(this::apply)
        .orElseThrow(() ->
            new NostrException(
                String.format("Valid KindTypeIf filter not specified, must be single exclusive of KindTypeIF [%s]",
                    Strings.join(acceptableKindTypes, ','))));

    return matchedKindType;
  }

  default void validateReferencedPubkeyTag(List<Filters> userProvidedFilterTypes) throws NostrException {
    final String RED_BOLD_BRIGHT = "\033[1;91m";
    final String GREEN_BOLD = "\033[1;32m";
    final String RESET = "\033[0m";
    String greenFont = GREEN_BOLD + "%s" + RESET;
    String redFont = RED_BOLD_BRIGHT + "%s" + RESET;

    List<Filterable> filterableStream = userProvidedFilterTypes.stream()
        .flatMap(filters ->
            filters.getFilterByType(ReferencedPublicKeyFilter.FILTER_KEY).stream()).toList();

    System.out.printf("contains req'd ReferencedPublicKeyFilter.FILTER_KEY? [%s]", !filterableStream.isEmpty() ?
        String.format(greenFont, "TRUE(ReqKindTypeServiceIF)") : String.format(redFont, "FALSE(ReqKindTypeServiceIF)"));

    filterableStream.stream()
        .findFirst().orElseThrow(() ->
            new EmptyFiltersException(
                userProvidedFilterTypes, String.format(redFont, "PubKeyTag")));
  }
  private Filterable apply(Filterable filterable1, Filterable filterable2) {
    throw new NostrException(
        String.format("Multiple matches found for Filterable [%s, %s]", filterable1.getFilterKey(), filterable2.getFilterKey()));
  }

  private KindTypeIF apply(KindTypeIF kindTypeIF1, KindTypeIF kindTypeIF2) {
    throw new NostrException(
        String.format("Multiple matches found for KindTypeIF [%s, %s]", kindTypeIF1, kindTypeIF2));
  }
}
