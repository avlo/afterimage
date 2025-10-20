package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.afterimage.InvalidReputationReqJsonException;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.user.Identity;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReputationRequestPlugin implements ReqKindPluginIF {
  public static final String REF_PUBKEY_FILTER_KEY = ReferencedPublicKeyFilter.FILTER_KEY;
  public static final String IDENTIFIER_TAG_FILTER_KEY = IdentifierTagFilter.FILTER_KEY;

  private final Identity aImgIdentity;

  @Autowired
  public ReputationRequestPlugin(@NonNull Identity aImgIdentity) {
    log.debug("loaded {} bean", getClass().getName());
    this.aImgIdentity = aImgIdentity;
  }

  @Override
  public Filters processIncomingRequest(@NonNull List<Filters> filtersList) throws NostrException {
    ReferencedPublicKeyFilter referencedPublicKeyFilter = filtersList.stream()
        .map(filters1 ->
            filters1.getFilterByType(REF_PUBKEY_FILTER_KEY))
        .flatMap(Collection::stream)
        .map(ReferencedPublicKeyFilter.class::cast)
        .findAny().orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, REF_PUBKEY_FILTER_KEY));

    AddressTagFilter addressTagFilter = new AddressTagFilter(
        new AddressTag(
            Kind.BADGE_DEFINITION_EVENT,
            aImgIdentity.getPublicKey(),
            filtersList.stream()
                .map(filters ->
                    filters.getFilterByType(IDENTIFIER_TAG_FILTER_KEY))
                .flatMap(Collection::stream)
                .map(IdentifierTagFilter.class::cast)
                .map(IdentifierTagFilter::getFilterable)
                .findAny().orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, IDENTIFIER_TAG_FILTER_KEY))));
    Filters filters = new Filters(
        new KindFilter(getKind()),
        referencedPublicKeyFilter,
        addressTagFilter);

    return filters;
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }
}
