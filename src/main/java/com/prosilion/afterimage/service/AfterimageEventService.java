//package com.prosilion.afterimage.service;
//
//import com.prosilion.afterimage.util.Factory;
//import com.prosilion.superconductor.service.event.EventServiceIF;
//import com.prosilion.superconductor.service.event.type.EventTypeServiceIF;
//import com.prosilion.superconductor.service.request.NotifierService;
//import com.prosilion.superconductor.service.request.pubsub.AddNostrEvent;
//import java.util.ArrayList;
//import java.util.List;
//import lombok.NonNull;
//import nostr.base.PublicKey;
//import nostr.event.BaseTag;
//import nostr.event.impl.GenericEvent;
//import nostr.event.message.EventMessage;
//import nostr.event.tag.PubKeyTag;
//import nostr.id.Identity;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Service
//public class AfterimageEventService<T extends GenericEvent> implements EventServiceIF<T> {
//  private final EventTypeServiceIF<T> eventTypeService;
//  private final NotifierService<T> notifierService;
//  private final Identity afterimageInstanceIdentity;
//
//  @Autowired
//  public AfterimageEventService(
//      @NonNull EventServiceIF<T> eventService,
//      @NonNull Identity afterimageInstanceIdentity,
//      @NonNull NotifierService<T> notifierService,
//      @NonNull EventTypeServiceIF<T> eventTypeService) {
//    this.notifierService = notifierService;
//    this.eventTypeService = eventTypeService;
//    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
//  }
//
//  public <U extends EventMessage> void processIncomingEvent(@NonNull U eventMessage) {
//    List<BaseTag> tags = new ArrayList<>();
//    PublicKey publicKey = getPublicKey(eventMessage);
//    PubKeyTag e = new PubKeyTag(publicKey);
//    tags.add(e);
//
//    GenericEvent textNoteEvent = Factory.createTextNoteEvent(afterimageInstanceIdentity, tags, CONTENT);
//    textNoteEvent.setKind(2112);
//    afterimageInstanceIdentity.sign(textNoteEvent);
//
//    eventTypeService.processIncomingEvent(event);
//    notifierService.nostrEventHandler(new AddNostrEvent<>(event));
//  }
//
//  private PublicKey getPublicKey(EventMessage eventMessage) {
//    return ((GenericEvent) eventMessage.getEvent()).getPubKey();
//  }
//}
