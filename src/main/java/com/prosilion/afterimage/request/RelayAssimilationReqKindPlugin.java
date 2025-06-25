package com.prosilion.afterimage.request;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.user.Identity;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RelayAssimilationReqKindPlugin implements ReqKindPlugin {

  @Autowired
  public RelayAssimilationReqKindPlugin(@NonNull Identity aImgIdentity) {
    log.debug("loaded RelayAssimilationReqKindTypePlugin bean");
  }

  @Override
  public Filters processIncomingRequest(@NonNull List<Filters> filtersList) {
    return new Filters(
        new KindFilter(getKind()));
  }

  @Override
  public Kind getKind() {
    return Kind.RELAY_DISCOVERY;
  }
}
