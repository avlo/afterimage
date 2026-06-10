package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.AuthorFilter;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BadgeDefinitionReputationRequestPlugin extends ReqKindTypePlugin {
  @Autowired
  public BadgeDefinitionReputationRequestPlugin(@NonNull Identity aImgIdentity) {
    super(aImgIdentity);
    log.debug("loaded BadgeDefinitionReputationRequestPlugin bean");
  }

  @Override
  public Filters processIncomingRequest(@NonNull List<Filters> filtersList) throws NostrException {
    return new Filters(
       new KindFilter(getKind()),
       matchFilterableKey(filtersList, AuthorFilter.FILTER_KEY),
       matchFilterableKey(filtersList, IdentifierTagFilter.FILTER_KEY)
//       ,
//       TODO: test below inclusion of matchFilterableKey(filtersList, ExternalIdentityTagFilter.FILTER_KEY)
//       matchFilterableKey(filtersList, ExternalIdentityTagFilter.FILTER_KEY)
    );
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_DEFINITION_EVENT;
  }

  @Override
  public KindTypeIF getKindType() {
    return AfterimageKindType.BADGE_DEFINITION_REPUTATION_KIND_TYPE;
  }
}
