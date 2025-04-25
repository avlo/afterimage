//package com.prosilion.afterimage.controller;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.prosilion.superconductor.controller.NostrEventController;
//import com.prosilion.superconductor.service.message.MessageService;
//import com.prosilion.superconductor.service.message.RelayInfoDocService;
//import java.util.List;
//import lombok.NonNull;
//import lombok.extern.slf4j.Slf4j;
//import nostr.event.BaseMessage;
//import nostr.event.json.codec.BaseMessageDecoder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//
//@Slf4j
//@Controller
//public class AfterImageController<T extends BaseMessage> extends NostrEventController<T> {
//  
//  @Autowired
//  public AfterImageController(
//      @NonNull List<MessageService<T>> messageServices,
//      @NonNull RelayInfoDocService relayInfoDocService,
//      @NonNull ApplicationEventPublisher publisher) {
//    super(messageServices, relayInfoDocService, publisher);
//  }
//
//  @Override
//  public void handleTextMessage(WebSocketSession session, TextMessage baseMessage) throws JsonProcessingException {
//    log.debug("Message from session [{}]", session.getId());
//    log.debug("Message content [{}]", baseMessage.getPayload());
//    T message = (T) new BaseMessageDecoder<>().decode(baseMessage.getPayload());
//  }
//}
