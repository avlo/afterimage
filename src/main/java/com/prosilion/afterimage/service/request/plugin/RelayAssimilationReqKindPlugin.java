package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.user.Identity;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RelayAssimilationReqKindPlugin implements ReqKindPluginIF<Kind> {
  @Getter
  private final Kind kind;

  @Autowired
  public RelayAssimilationReqKindPlugin(@NonNull Identity aImgIdentity) {
    log.debug("loaded RelayAssimilationReqKindTypePlugin bean");
    this.kind = Kind.RELAY_DISCOVERY;
  }

  @Override
  public Filters processIncomingRequest(@NonNull List<Filters> filtersList) {
    return new Filters(
        new KindFilter(getKind()));
  }
}
