package com.prosilion.afterimage.request;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.util.InvalidReputationReqJsonException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReputationReqKindTypePlugin extends ReqKindTypePlugin {

  public static final String REF_PUBKEY_FILTER_KEY = ReferencedPublicKeyFilter.FILTER_KEY;
  public static final String KIND_FILTER_KEY = KindFilter.FILTER_KEY;
  public static final String IDENTIFIER_TAG_FILTER_KEY = IdentifierTagFilter.FILTER_KEY;

  @Autowired
  public ReputationReqKindTypePlugin(@NonNull Identity aImgIdentity) {
    super(aImgIdentity);
    log.debug("loaded ReputationReqKindTypePlugin bean");
  }

//  TODO: addressTag filters variant, see REPUTATION.todo line 187, reputation request message: 
//  public Filters processIncomingRequestOld(@NonNull List<Filters> filtersList) {
//    Filters filters = new Filters(
//        validateRequiredFilterKindType(filtersList).stream().map(publicKey ->
//                new AddressTagFilter(
//                    getAddressTag(
//                        publicKey,
//                        super.getAImgIdentity())))
//            .map(Filterable.class::cast)
//            .toList());
//    return filters;
//  }

  @Override
  public Filters processIncomingRequest(@NonNull List<Filters> filtersList) {
    return cullRequiredFilters(filtersList);
  }

  /**
   * in addition to confirming proper filters exist, should strip away superfluous filters from List<Filters>
   */
//  TODO: addressTag filters variant, see REPUTATION.todo line 187, reputation request message:  
//  private List<PublicKey> validateRequiredFilterKindType(List<Filters> filtersList) throws InvalidReputationReqJsonException {
//    return filtersList.stream()
//        .flatMap(filters ->
//            Optional.of(filters.getFilterByType(getKind().getName()).stream())
//                .orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, getKind().getName())))
//        .map(AddressTagFilter.class::cast)
//        .map(AddressTagFilter::getFilterable)
//        .filter(addressTag ->
//            Optional.of(
//                    addressTag.getKind().equals(getKind()))
////            note: below will throw exception if any filter among all filters does not contain 2113
////              may want to change/update this, revisit later
//                .orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, getKind().getName())))
//        .map(AddressTag::getPublicKey)
//        .toList();
//  }

  /**
   * in addition to confirming proper filters exist, should strip away superfluous filters from List<Filters>
   */
  private Filters cullRequiredFilters(List<Filters> filtersList) throws InvalidReputationReqJsonException {
//    Collectors.teeing(
    Filterable kindFilterable = filtersList.stream()
        .flatMap(filters ->
            Optional.of(filters.getFilterByType(KIND_FILTER_KEY).stream())
                .orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, KIND_FILTER_KEY)))
        .map(KindFilter.class::cast)
//        .filter(kind ->
//            Optional.of(kind.getName().equalsIgnoreCase(getKindType().getName()))
//                .orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, getKindType().getName())))
        .findFirst().orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, getKindType().getName()));

    List<Filterable> pubkeyTagFilterables = filtersList.stream()
        .flatMap(filters ->
            Optional.of(filters.getFilterByType(REF_PUBKEY_FILTER_KEY).stream())
                .orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, REF_PUBKEY_FILTER_KEY)))
        .map(ReferencedPublicKeyFilter.class::cast)
        .map(Filterable.class::cast)
        .toList();

    List<Filterable> identifierTagFilterables = filtersList.stream()
        .flatMap(filters ->
            Optional.of(filters.getFilterByType(IDENTIFIER_TAG_FILTER_KEY).stream())
                .orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, IDENTIFIER_TAG_FILTER_KEY)))
        .map(IdentifierTagFilter.class::cast)
        .map(IdentifierTagFilter::getFilterable)
        .filter(filterable ->
            Optional.of(filterable.getUuid().equalsIgnoreCase(getKindType().getName()))
                .orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, getKindType().getName())))
        .map(String.class::cast)
        .map(result -> new IdentifierTag(String.valueOf(result)))
        .map(IdentifierTagFilter::new)
        .map(Filterable.class::cast)
        .toList();

    List<Filterable> list = Stream.concat(
        Stream.of(kindFilterable),
        Stream.concat(
            identifierTagFilterables.stream(),
            pubkeyTagFilterables.stream())).toList();

    Filters filters = new Filters(list);

    return filters;
  }

//  private AddressTag getAddressTag(PublicKey publicKey, Identity aImgIdentity) {
//    return new AddressTag(
//        getKind(),
//        publicKey,
//        new IdentifierTag(

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }

  @Override
  public KindTypeIF getKindType() {
    return AfterimageKindType.REPUTATION;
  }
}
