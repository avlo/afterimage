//package com.prosilion.afterimage.config.eventaux;
//
//import com.prosilion.nostr.enums.Kind;
//import com.prosilion.nostr.event.EventIF;
//import com.prosilion.nostr.event.internal.Relay;
//import com.prosilion.nostr.message.EventMessage;
//import com.prosilion.nostr.tag.ExternalIdentityTag;
//import com.prosilion.superconductor.base.service.event.EventService;
//import com.prosilion.superconductor.base.service.event.EventServiceIF;
//import lombok.NonNull;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//public class EventAuxService implements EventServiceIF {
//  private final EventService eventService;
//  private final EventAuxPlugin eventAuxPlugin;
//
//  public EventAuxService(
//     @NonNull EventService eventService,
//     @NonNull EventAuxPlugin eventAuxPlugin) {
//    this.eventService = eventService;
//    this.eventAuxPlugin = eventAuxPlugin;
//  }
//
//  @Override
//  public void processIncomingEvent(@NonNull EventMessage eventMessage, @NonNull Relay relay) {
//    EventIF event = eventMessage.getEvent();
//    Kind kind = event.getKind();
//    log.debug("processIncomingEvent(EventMessage) kind:[{}]\n{}", kind, event.createPrettyPrintJson());
//
//    if (matchesAuxKind(event) && excludesExternalIdentityTag(event)) {
//      eventAuxPlugin.processIncomingEvent(event, relay);
//      return;
//    }
//
//    eventService.processIncomingEvent(eventMessage, relay);
//  }
//
//  private boolean matchesAuxKind(EventIF event) {
//    return eventAuxPlugin.getEventAuxKindMaterializers()
//       .keySet().stream().anyMatch(event.getKind()::equals);
//  }
//
//  private boolean excludesExternalIdentityTag(EventIF event) {
//    return event.asGenericEventRecord().getTypeSpecificTags(ExternalIdentityTag.class).isEmpty();
//  }
//}
