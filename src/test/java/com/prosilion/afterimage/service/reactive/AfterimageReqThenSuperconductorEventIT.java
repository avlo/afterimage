//package com.prosilion.afterimage.service.reactive;
//
//import com.prosilion.afterimage.service.CommonContainer;
//import com.prosilion.afterimage.relay.AfterimageMeshRelayService;
//import com.prosilion.afterimage.util.Factory;
//import com.prosilion.afterimage.util.TestSubscriber;
//import com.prosilion.superconductor.service.event.EventService;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import lombok.NonNull;
//import lombok.extern.slf4j.Slf4j;
//import nostr.event.BaseMessage;
//import nostr.event.BaseTag;
//import nostr.event.filter.Filters;
//import nostr.event.filter.ReferencedPublicKeyFilter;
//import nostr.event.filter.VoteTagFilter;
//import nostr.event.impl.GenericEvent;
//import nostr.event.message.EventMessage;
//import nostr.event.message.OkMessage;
//import nostr.event.message.ReqMessage;
//import nostr.event.tag.PubKeyTag;
//import nostr.event.tag.VoteTag;
//import nostr.id.Identity;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
//import org.springframework.test.context.ActiveProfiles;
//
//import static com.prosilion.afterimage.service.reactive.SuperconductorEventThenAfterimageReqIT.getGenericEvents;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//@Slf4j
//@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
//@ActiveProfiles("test")
//class AfterimageReqThenSuperconductorEventIT extends CommonContainer {
//  private final AfterimageMeshRelayService superconductorRelayReactiveClient;
//  private final AfterimageMeshRelayService afterimageMeshRelayService;
//  private final EventService<GenericEvent> eventService;
//
//  private final Identity identity = Factory.createNewIdentity();
//  private final VoteTag voteTag = new VoteTag(1);
//  private final static int KIND = 2112;
//
//  private final static String subscriberId = Factory.generateRandomHex64String();
//
//  @Autowired
//  AfterimageReqThenSuperconductorEventIT(
//      @NonNull EventService<GenericEvent> eventService,
//      @NonNull @Value("${superconductor.lone.relay.url}") String superconductorRelayUri,
//      @NonNull AfterimageMeshRelayService afterimageMeshRelayService
//  ) {
////    String serviceHost = superconductorContainer.getServiceHost("superconductor-afterimage", 5555);
////    log.debug("SuperconductorEventThenAfterimageReqIT host: {}", serviceHost);
//    log.debug("SuperconductorEventThenAfterimageReqIT hash: {}", superconductorRelayUri.hashCode());
//    this.superconductorRelayReactiveClient = new AfterimageMeshRelayService(superconductorRelayUri);
//    this.afterimageMeshRelayService = afterimageMeshRelayService;
//    this.eventService = eventService;
//  }
//
//  @Test
//  void testAfterimageReqThenSuperconductorTwoEvents() throws IOException {
//    final Identity authorIdentity = Factory.createNewIdentity();
////    // # --------------------- Aimg EVENT -------------------
////    // query Aimg for (as yet to be impl'd) reputation score event
////    //   results should process at end of test once pre-req SC events have completed
//    TestSubscriber<BaseMessage> afterImageEventsSubscriber = new TestSubscriber<>();
//    afterimageMeshRelayService.send(
//        new ReqMessage(
//            subscriberId,
//            new Filters(
//                new ReferencedPublicKeyFilter<>(new PubKeyTag(authorIdentity.getPublicKey())),
//                new VoteTagFilter<>(voteTag))), afterImageEventsSubscriber);
//
//    // # --------------------- SC EVENT 1 of 2-------------------
//    //    begin event creation for submission to SC
//    List<BaseTag> tags = new ArrayList<>();
//    tags.add(voteTag);
//    tags.add(new PubKeyTag(authorIdentity.getPublicKey()));
//
//    GenericEvent textNoteEvent_1 = Factory.createVoteEvent(
//        identity, tags,
//        Factory.lorumIpsum(AfterimageReqThenSuperconductorEventIT.class));
//    textNoteEvent_1.setKind(KIND);
//    identity.sign(textNoteEvent_1);
//
//    //    submit subscriber's first Event to superconductor
//    TestSubscriber<OkMessage> okMessageSubscriber_1 = new TestSubscriber<>();
//    superconductorRelayReactiveClient.send(new EventMessage(textNoteEvent_1), okMessageSubscriber_1);
//    assertEquals(true, okMessageSubscriber_1
//        .getItems()
//        .getFirst()
//        .getFlag());
//    log.debug("received 1of2 OkMessage...");
//
//    GenericEvent textNoteEvent_2 = Factory.createVoteEvent(
//        identity, tags,
//        Factory.lorumIpsum(AfterimageReqThenSuperconductorEventIT.class));
//    textNoteEvent_2.setKind(KIND);
//    identity.sign(textNoteEvent_2);
//
//    // # --------------------- SC EVENT 2 of 2-------------------
//
//    TestSubscriber<OkMessage> okMessageSubscriber = new TestSubscriber<>();
//    superconductorRelayReactiveClient.send(new EventMessage(textNoteEvent_2), okMessageSubscriber);
//
//    assertEquals(true, okMessageSubscriber_1
//        .getItems()
//        .getFirst()
//        .getFlag());
//    log.debug("received 2of2 OkMessage...");
//
//    // # --------------------- SC REQ -------------------
//    //    submit matching author & vote tag Req to superconductor
//
//    TestSubscriber<BaseMessage> superConductorEventsSubscriber = new TestSubscriber<>();
//    superconductorRelayReactiveClient.send(
//        new ReqMessage(
//            subscriberId,
//            new Filters(
//                new ReferencedPublicKeyFilter<>(new PubKeyTag(authorIdentity.getPublicKey())),
//                new VoteTagFilter<>(voteTag))), superConductorEventsSubscriber);
//
//    List<BaseMessage> returnedSuperconductorBaseMessages = superConductorEventsSubscriber.getItems();
//    List<GenericEvent> returnedSuperconductorEvents = getGenericEvents(returnedSuperconductorBaseMessages);
//
//    assertTrue(returnedSuperconductorEvents.stream().anyMatch(genericEvent -> genericEvent.getId().equals(textNoteEvent_1.getId())));
//    assertTrue(returnedSuperconductorEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(textNoteEvent_1.getContent())));
//    assertTrue(returnedSuperconductorEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(textNoteEvent_2.getContent())));
//    assertTrue(returnedSuperconductorEvents.stream().anyMatch(genericEvent -> genericEvent.getPubKey().toHexString().equals(textNoteEvent_1.getPubKey().toHexString())));
//    assertEquals(returnedSuperconductorEvents.getFirst().getKind(), textNoteEvent_1.getKind());
//    assertTrue(returnedSuperconductorEvents.stream().anyMatch(genericEvent -> genericEvent.getKind().equals(textNoteEvent_1.getKind())));
//
//    //    save SC result to Aimg
//    //    should trigger Aimg afterImageEventsSubscriber
//    returnedSuperconductorEvents.forEach(event ->
//        eventService.processIncomingEvent(new EventMessage(event)));
//
//    // # --------------------- Aimg EVENTS returned -------------------
//    List<BaseMessage> returnedAfterImageReqMessages = afterImageEventsSubscriber.getItems();
//    List<GenericEvent> afterImageEvents = getGenericEvents(returnedAfterImageReqMessages);
//    log.debug("afterimage returned events:");
//    afterImageEvents.forEach(genericEvent -> log.debug(genericEvent.getId()));
//    assertTrue(afterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(textNoteEvent_1.getContent())));
//    assertTrue(afterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(textNoteEvent_2.getContent())));
//  }
//}
