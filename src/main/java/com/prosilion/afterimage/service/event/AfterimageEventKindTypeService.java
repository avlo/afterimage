//package com.prosilion.afterimage.service.event;
//
//import com.prosilion.nostr.enums.Kind;
//import com.prosilion.nostr.event.EventIF;
//import com.prosilion.superconductor.base.service.event.service.EventKindTypeServiceIF;
//import com.prosilion.superconductor.base.service.event.type.KindTypeIF;
//import java.util.List;
//import org.springframework.lang.NonNull;
//
//public class AfterimageEventKindTypeService implements EventKindTypeServiceIF {
//  EventKindTypeServiceIF eventKindTypeService;
//
//  public AfterimageEventKindTypeService(@NonNull EventKindTypeServiceIF eventKindTypeService) {
//    this.eventKindTypeService = eventKindTypeService;
//  }
//
//  @Override
//  public List<KindTypeIF> getKindTypes() {
//    return eventKindTypeService.getKindTypes();
//  }
//
////  @Override
////  public KindTypeIF getKindType(EventIF event) {
////    Optional<KindTypeIF> first = getKindTypes().stream().filter(kindTypeIF ->
////        kindTypeIF.getName().equals(Filterable.getTypeSpecificTagsStream(AddressTag.class, event)
////            .findFirst()
////            .map(AddressTag::getIdentifierTag).orElseThrow()
////            .getUuid())).findFirst();
////    KindTypeIF orElse = first.orElse(AfterimageKindType.UNIT_VOTE);
////    return orElse;
////  }
//
//  @Override
//  public void processIncomingEvent(EventIF eventIF) {
//    eventKindTypeService.processIncomingEvent(eventIF);
//  }
//
//  @Override
//  public List<Kind> getKinds() {
//    return eventKindTypeService.getKinds();
//  }
//}
