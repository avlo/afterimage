package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.afterimage.InvalidReputationReqJsonException;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReputationRequestPlugin extends ReqKindTypePlugin {
  public static final String REF_PUBKEY_FILTER_KEY = ReferencedPublicKeyFilter.FILTER_KEY;

  @Autowired
  public ReputationRequestPlugin(@NonNull Identity aImgIdentity) {
    super(aImgIdentity);
    log.debug("loaded ReputationReqKindTypePlugin bean");
  }

  @Override
  public Filters processIncomingRequest(@NonNull List<Filters> filtersList) throws NostrException {
    log.debug("processIncoming(List<Filters>)\n  with List<Filters>:\n{}",
        filtersList.stream()
            .map(filters -> filters.toString(2))
            .collect(Collectors.joining(",\n")));

    List<Filterable> filterableStream = filtersList.stream()
        .map(filters1 ->
            filters1.getFilterByType(REF_PUBKEY_FILTER_KEY))
        .flatMap(Collection::stream).toList();

//    final String RED_BOLD_BRIGHT = "\033[1;91m";
//    final String GREEN_BOLD = "\033[1;32m";
//    final String RESET = "\033[0m";
//    String greenFont = GREEN_BOLD + "%s" + RESET;
//    String redFont = RED_BOLD_BRIGHT + "%s" + RESET;

    log.debug("contains req'd ReferencedPublicKeyFilter.FILTER_KEY? [ {} ]", !filterableStream.isEmpty() ?
//        String.format(greenFont, "TRUE(ReptationRequestPlugin)") : String.format(redFont, "FALSE(ReptationRequestPlugin)"));
        "TRUE(ReputationRequestPlugin)" : "FALSE(ReputationRequestPlugin)");

    ReferencedPublicKeyFilter referencedPublicKeyFilter = filterableStream.stream()
        .map(ReferencedPublicKeyFilter.class::cast)
        .findAny().orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, REF_PUBKEY_FILTER_KEY.concat(" tag")));

    AddressTagFilter addressTagFilter = getAddressTagFilter(filtersList);

    Filters filters = new Filters(
        new KindFilter(getKind()),
        referencedPublicKeyFilter,
        addressTagFilter);

    return filters;
  }

  private AddressTagFilter getAddressTagFilter(List<Filters> filtersList) {
//    return getTagFilterViaIdentifierTag(filtersList);
    return getTagFilterViaAddressTagTag(filtersList);
  }

  private AddressTagFilter getTagFilterViaAddressTagTag(List<Filters> filtersList) {
    AddressTagFilter addressTagFilter = filtersList.stream()
        .map(filters ->
            filters.getFilterByType(AddressTagFilter.FILTER_KEY))
        .flatMap(Collection::stream)
        .map(AddressTagFilter.class::cast)
        .findAny().orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, AddressTagFilter.FILTER_KEY.concat(" tag")));
    return addressTagFilter;
  }

  private AddressTagFilter getTagFilterViaIdentifierTag(List<Filters> filtersList) {
    AddressTagFilter addressTagFilter = new AddressTagFilter(
        new AddressTag(
            Kind.BADGE_DEFINITION_EVENT,
            getAImgIdentity().getPublicKey(),
            filtersList.stream()
                .map(filters ->
                    filters.getFilterByType(IdentifierTagFilter.FILTER_KEY))
                .flatMap(Collection::stream)
                .map(IdentifierTagFilter.class::cast)
                .map(IdentifierTagFilter::getFilterable)
                .findAny().orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, IdentifierTagFilter.FILTER_KEY.concat(" tag")))));
    return addressTagFilter;
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }

  @Override
  public KindTypeIF getKindType() {
    return AfterimageKindType.BADGE_AWARD_REPUTATION_KIND_TYPE;
  }
}
