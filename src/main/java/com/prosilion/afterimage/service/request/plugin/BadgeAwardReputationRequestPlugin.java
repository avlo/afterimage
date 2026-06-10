package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.ExternalIdentityTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BadgeAwardReputationRequestPlugin extends ReqKindTypePlugin {
  @Autowired
  public BadgeAwardReputationRequestPlugin(@NonNull Identity aImgIdentity) {
    super(aImgIdentity);
    log.debug("loaded BadgeAwardReputationRequestPlugin bean");
  }

  @Override
  public Filters processIncomingRequest(@NonNull List<Filters> filtersList) throws NostrException {
    return new Filters(
       new KindFilter(getKind()),
       matchFilterableKey(filtersList, ReferencedPublicKeyFilter.FILTER_KEY)
// TODO: test below inclusion of matchFilterableKey(filtersList, ExternalIdentityTagFilter.FILTER_KEY)
//   where ExternalIdentityTagFilter proof value comes from ctor(aImgIdentity) parameter
       , matchFilterableKey(filtersList, ExternalIdentityTagFilter.FILTER_KEY)

// TODO: revisit below AddressTagFilter (repDefnCreator) inclusion/excluson
//   , matchFilterableKey(filtersList, AddressTagFilter.FILTER_KEY)
    );
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
