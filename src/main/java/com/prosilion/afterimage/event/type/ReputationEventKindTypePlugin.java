package com.prosilion.afterimage.event.type;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.GenericEventKindTypeIF;
import com.prosilion.superconductor.service.event.type.AbstractPublishingEventKindPlugin;
import com.prosilion.superconductor.service.event.type.AbstractPublishingEventKindTypePlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
// TODO: this class is necessary solely for its getKind() REPUTATION.  potential refactor
public class ReputationEventKindTypePlugin extends AbstractPublishingEventKindTypePlugin {

  @Autowired
  public ReputationEventKindTypePlugin(AbstractPublishingEventKindPlugin abstractPublishingEventKindPlugin) {
    super(abstractPublishingEventKindPlugin);
    log.debug("ReputationEventKindTypePlugin loaded");
  }

  @Override
  public Kind getKind() {
    log.debug("ReputationEventKindTypePlugin getKind returning Kind.BADGE_AWARD_EVENT");
    return Kind.BADGE_AWARD_EVENT;
  }

  @Override
  public KindTypeIF getKindType() {
    log.debug("ReputationEventKindTypePlugin getKindType returning Kind.REPUTATION");
    return AfterimageKindType.REPUTATION;
  }

  @Override
  public void processIncomingPublishingEventKindType(@NonNull GenericEventKindTypeIF event) {
    log.debug("ReputationEventKindTypePlugin processIncomingPublishingEventKindType REPUTATION event: [{}]", event);
    super.processIncomingEvent(event);
  }
}
