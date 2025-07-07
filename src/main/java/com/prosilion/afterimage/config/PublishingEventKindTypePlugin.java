package com.prosilion.afterimage.config;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.GenericEventKindIF;
import com.prosilion.superconductor.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.service.request.NotifierService;
import com.prosilion.superconductor.service.request.pubsub.AddNostrEvent;
import org.springframework.lang.NonNull;

// our CarDecorator for PublishingEventKindType hierarchy
public class PublishingEventKindTypePlugin implements EventKindTypePluginIF<KindTypeIF> {
  private final NotifierService notifierService;
  private final EventKindTypePluginIF<KindTypeIF> eventKindTypePlugin;

  public PublishingEventKindTypePlugin(@NonNull NotifierService notifierService, @NonNull EventKindTypePluginIF<KindTypeIF> eventKindTypePlugin) {
    this.notifierService = notifierService;
    this.eventKindTypePlugin = eventKindTypePlugin;
  }

  public void processIncomingEvent(@NonNull GenericEventKindIF event) {
    this.eventKindTypePlugin.processIncomingEvent(event);
    this.notifierService.nostrEventHandler(new AddNostrEvent(event));
  }

  public Kind getKind() {
    return this.eventKindTypePlugin.getKind();
  }

  public KindTypeIF getKindType() {
    return this.eventKindTypePlugin.getKindType();
  }
}
