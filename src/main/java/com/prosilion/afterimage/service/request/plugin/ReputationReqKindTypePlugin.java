package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.afterimage.InvalidReputationReqJsonException;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReputationReqKindTypePlugin extends ReqKindTypePlugin {
  public static final String REF_PUBKEY_FILTER_KEY = ReferencedPublicKeyFilter.FILTER_KEY;
  public static final String IDENTIFIER_TAG_FILTER_KEY = IdentifierTagFilter.FILTER_KEY;
  public static final String ADDRESS_TAG_FILTER_KEY = AddressTagFilter.FILTER_KEY;

  @Autowired
  public ReputationReqKindTypePlugin(@NonNull Identity aImgIdentity) {
    super(AfterimageKindType.REPUTATION, aImgIdentity);
    log.debug("loaded ReputationReqKindTypePlugin bean");
  }

  @Override
  public Filters processIncomingRequest(@NonNull List<Filters> filtersList) throws NostrException {
    ReferencedPublicKeyFilter referencedPublicKeyFilter = filtersList.stream()
        .map(filters ->
            filters.getFilterByType(REF_PUBKEY_FILTER_KEY))
        .flatMap(Collection::stream)
        .map(ReferencedPublicKeyFilter.class::cast)
        .findAny().orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, REF_PUBKEY_FILTER_KEY));

    IdentifierTag identifierTag = filtersList.stream()
        .map(filters ->
            filters.getFilterByType(IDENTIFIER_TAG_FILTER_KEY))
        .flatMap(Collection::stream)
        .map(IdentifierTagFilter.class::cast)
        .map(IdentifierTagFilter::getFilterable)
        .findAny().orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, IDENTIFIER_TAG_FILTER_KEY));

//    AddressTag addressTag = filtersList.stream()
//        .map(filters ->
//            filters.getFilterByType(ADDRESS_TAG_FILTER_KEY))
//        .flatMap(Collection::stream)
//        .map(AddressTagFilter.class::cast)
//        .map(AddressTagFilter::getFilterable)
//        .findAny().orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, ADDRESS_TAG_FILTER_KEY));
//
//    Optional.of(
//            Optional.ofNullable(
//                    addressTag.getIdentifierTag())
//                .orElseThrow(() ->
//                    new InvalidReputationReqJsonException(filtersList, getKindType().getName())).getUuid())
//        .filter(uuid ->
//            uuid.equalsIgnoreCase(getKindType().getName())).orElseThrow(() ->
//            new InvalidReputationReqJsonException(filtersList, getKindType().getName()));

    return
        new Filters(
            new KindFilter(Kind.BADGE_AWARD_EVENT),
            referencedPublicKeyFilter,
            new AddressTagFilter(
                new AddressTag(
                    Kind.BADGE_DEFINITION_EVENT,
                    getAImgIdentity().getPublicKey(),
//                    addressTag.identifierTag()
                    identifierTag
                )));
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }

  @Override
  public KindTypeIF getKindType() {
    return AfterimageKindType.REPUTATION;
  }
}
