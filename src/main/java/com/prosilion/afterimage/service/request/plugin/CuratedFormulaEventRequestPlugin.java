package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.nostr.enums.Kind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CuratedFormulaEventRequestPlugin extends AbstractBadgeAwardEventRequestPlugin {
  @Override
  public Kind getKind() {
    return Kind.CURATION_SETS_FORMULA_EVENT; // kind 30_006
  }
}
