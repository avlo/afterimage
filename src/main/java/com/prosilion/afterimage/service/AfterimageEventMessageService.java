//package com.prosilion.afterimage.service;
//
//import com.prosilion.superconductor.service.clientresponse.ClientResponseService;
//import com.prosilion.superconductor.service.message.event.EventMessageServiceIF;
//import lombok.NonNull;
//import lombok.extern.slf4j.Slf4j;
//import nostr.event.impl.GenericEvent;
//import nostr.event.message.EventMessage;
//import nostr.id.Identity;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Slf4j
//@Service
//public class AfterimageEventMessageService<T extends EventMessage> implements EventMessageServiceIF<T> {
//  //  TODO: revisit below, may/best move SC's EventServiceIF into autoconfigure (like done w/ requestmsg)
////  private final EventServiceIF<GenericEvent> eventService;
//  private final AfterimageEventService<GenericEvent> afterimageEventService;
//  private final ClientResponseService clientResponseService;
//
//  @Autowired
//  public AfterimageEventMessageService(
//      @NonNull Identity identity,
////  TODO: related above todo
////      @NonNull EventServiceIF<GenericEvent> eventService,
//      @NonNull AfterimageEventService<GenericEvent> afterimageEventService,
//      @NonNull ClientResponseService clientResponseService) {
////    TODO: related above TODO
////    this.eventService = eventService;
//    this.afterimageEventService = afterimageEventService;
//    this.clientResponseService = clientResponseService;
//  }
//
//  @Override
//  public void processIncoming(@NonNull T eventMessage, @NonNull String sessionId) {
//    afterimageEventService.processIncomingEvent(eventMessage);
//    processOkClientResponse(eventMessage, sessionId);
//  }
//
//  @Override
//  public void processOkClientResponse(@NonNull T eventMessage, @NonNull String sessionId) {
//    clientResponseService.processOkClientResponse(sessionId, eventMessage);
//  }
//
//  @Override
//  public void processNotOkClientResponse(@NonNull T eventMessage, @NonNull String sessionId, @NonNull String errorMessage) {
//    clientResponseService.processNotOkClientResponse(sessionId, new EventMessage(eventMessage.getEvent()), errorMessage);
//  }
//}
