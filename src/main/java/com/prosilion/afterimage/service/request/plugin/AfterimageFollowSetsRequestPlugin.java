package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AfterimageFollowSetsRequestPlugin implements ReqKindPluginIF { // kind 30_000
  public AfterimageFollowSetsRequestPlugin() {
    log.debug("loaded {} bean", getClass().getSimpleName());
  }

  @Override
  public Filters processIncomingRequest(@NonNull List<Filters> filtersList) {
    log.debug("0000000000000000000");
    log.debug("0000000000000000000");
    log.debug("{} processing incoming Kind.FOLLOW_SETS 30_000 event", getClass().getSimpleName());
    log.debug("0000000000000000000");
    log.debug("0000000000000000000");
    return new Filters(new KindFilter(getKind()));
  }

  @Override
  public Kind getKind() {
    return Kind.FOLLOW_SETS; // kind 30_000
  }
}
