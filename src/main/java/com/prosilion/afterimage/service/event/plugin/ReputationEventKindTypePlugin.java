package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.GenericEventKindIF;
import com.prosilion.nostr.event.GenericEventKindTypeIF;
import com.prosilion.superconductor.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.service.event.type.PublishingEventKindTypePlugin;
import com.prosilion.superconductor.service.request.NotifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
public class ReputationEventKindTypePlugin extends PublishingEventKindTypePlugin {

  public ReputationEventKindTypePlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindTypePluginIF<KindTypeIF> eventKindTypePlugin) {
    super(notifierService, eventKindTypePlugin);
    log.debug("ReputationEventKindTypePlugin loaded");
  }

//  @Override
//  public Kind getKind() {
//    log.debug("ReputationEventKindTypePlugin getKind returning Kind.BADGE_AWARD_EVENT");
//    return Kind.BADGE_AWARD_EVENT;
//  }
//
//  @Override
//  public KindTypeIF getKindType() {
//    log.debug("ReputationEventKindTypePlugin getKindType returning Kind.REPUTATION");
//    return AfterimageKindType.REPUTATION;
//  }

  //  TODO: below may/should be superfluous
  @Override
  public void processIncomingEvent(@NonNull GenericEventKindIF event) {
    super.processIncomingEvent(event);
  }

  //  TODO: below may/should be superfluous  
  @Override
  public void processIncomingEvent(@NonNull GenericEventKindTypeIF event) {
    super.processIncomingEvent(event);
  }
}
