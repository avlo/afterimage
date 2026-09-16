package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.nostr.enums.Kind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BadgeSetsEventRequestPlugin extends AbstractBadgeAwardEventRequestPlugin {
  @Override
  public Kind getKind() {
    return Kind.BADGE_SETS_EVENT; // kind 30_008
  }
}
