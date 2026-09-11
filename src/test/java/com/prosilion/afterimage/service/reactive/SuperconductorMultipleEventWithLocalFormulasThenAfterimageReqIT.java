//package com.prosilion.afterimage.service.reactive;
//
//import com.prosilion.afterimage.config.SingleContainerTestConfig;
//import com.prosilion.nostr.NostrException;
//import com.prosilion.nostr.event.BadgeAwardGenericEvent;
//import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
//import com.prosilion.nostr.event.BaseEvent;
//import com.prosilion.nostr.event.EventIF;
//import com.prosilion.nostr.event.FormulaEvent;
//import com.prosilion.nostr.event.GenericEventRecord;
//import com.prosilion.nostr.event.curated.CuratedFormulaEvent;
//import com.prosilion.nostr.event.internal.Relay;
//import com.prosilion.nostr.filter.Filters;
//import com.prosilion.nostr.message.BaseMessage;
//import com.prosilion.nostr.tag.PubKeyTag;
//import com.prosilion.nostr.tag.ReferenceTag;
//import com.prosilion.nostr.user.Identity;
//import com.prosilion.nostr.user.PublicKey;
//import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
//import java.util.List;
//import lombok.NonNull;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.MethodOrderer;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestMethodOrder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.ActiveProfiles;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@Slf4j
//@TestMethodOrder(MethodOrderer.MethodName.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
//@ActiveProfiles("test")
//@Import(SingleContainerTestConfig.class)
//public class SuperconductorMultipleEventWithLocalFormulasThenAfterimageReqIT extends AbstractIT {
//  @Autowired
//  public SuperconductorMultipleEventWithLocalFormulasThenAfterimageReqIT(
//     @NonNull Identity afterimageInstanceIdentity,
//     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
//     @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) {
//    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrl);
//  }
//
//  @Test
//  void aSuperconductorEventThenAfterimageReq() throws NostrException {
//    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> upvoteEvent_1 = createUpvoteEvent(superconductorRelay);
//
//    EventIF simulateIncomingUpvoteEvent_1 = submitSCEvent(
//       upvoteEvent_1,
//       superconductorRelayUrl,
//       upvoteAndOrDownvoteEventFilter);
//
//    submitRelayEventWithDuration_backup(simulateIncomingUpvoteEvent_1, afterimageRelayUrl);
//
//    assertEquals(
//       "1",
//       submitAfterImageReq(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl).getFirst().getContent());
//
////    below is different recipient, works fine
////    PublicKey newRecipient = Identity.generateRandomIdentity().getPublicKey();
////    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> upvoteEvent2 = createUpvoteEvent(superconductorRelay, newRecipient);
////
////    EventIF simulateIncomingUpvoteEvent_2 = submitSCEvent(
////       upvoteEvent2,
////       superconductorRelayUrl,
////       upvoteAndOrDownvoteEventFilter);
////
////    submitRelayEventWithDuration_backup(simulateIncomingUpvoteEvent_2, afterimageRelayUrl);
////
////    assertEquals(
////       "1",
////       submitAfterImageReq(new PubKeyTag(newRecipient), afterimageRelayUrl).getFirst().getContent());
//
////    below is same 1st recipient, fails    
//    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> upvoteEvent_3 = createUpvoteEvent(superconductorRelay);
//
//    EventIF simulateIncomingUpvoteEvent_3 = submitSCEvent(
//       upvoteEvent_3,
//       superconductorRelayUrl,
//       upvoteAndOrDownvoteEventFilter);
//
//    submitRelayEventWithDuration_backup(simulateIncomingUpvoteEvent_3, afterimageRelayUrl);
//
//    List<EventIF> eventIFS = submitAfterImageReq(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl);
//    System.out.println(eventIFS.stream().map(EventIF::createPrettyPrintJson));
//
//    assertEquals(
//       "2",
//       submitAfterImageReq(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl).getFirst().getContent());
//  }
//
//  protected EventIF submitSCEventCheckEventTagEventId(BaseEvent event, String url, Filters filters, PublicKey publicKey) {
////  submit first Event to superconductor
//    submitRelayEventWithDuration_backup(event, url);
////  sanity check event submissions processed by superconductor
//    List<BaseMessage> baseMessages = new NostrSingleRequestService().send(
//       createSuperconductorReqMessageEvent(generateRandomHex64String(), filters), url);
//
//    log.debug("retrieved superconductor events:");
//    List<EventIF> receivedEventIFs = getGenericEvents(baseMessages);
//    receivedEventIFs.stream().map(EventIF::asGenericEventRecord).map(GenericEventRecord::createPrettyPrintJson).forEach(log::debug);
//
//    EventIF upvoteEventIF = receivedEventIFs.getFirst();
//
////    assertEquals(event.getId(), upvoteEventIF.requireFirstTag(EventTag.class).getEventId());
//    assertEquals(publicKey, upvoteEventIF.getPublicKey());
//    assertEquals(upvoteEventIF.getContent(), event.getContent());
////    assertEquals(upvoteEventIF.getPublicKey().toHexString(), event.getPublicKey().toHexString());
//    assertEquals(upvoteEventIF.getKind(), event.getKind());
//
//    return upvoteEventIF;
//  }
//
//  @Override
//  protected CuratedFormulaEvent createCuratedFormulaPlusOneEvent() {
//    return new CuratedFormulaEvent(
//       afterimageInstanceIdentity,
//       new FormulaEvent(
//          formulaCreator,
//          formulaUpvoteIdentifierTag,
//          awardUpvoteDefinitionEvent,
//          PLUS_ONE_FORMULA,
//          new Relay(afterimageRelayUrl)),
//       new ReferenceTag(afterimageRelayUrl),
//       new Relay(afterimageRelayUrl));
//  }
//
//  @Override
//  protected CuratedFormulaEvent createCuratedFormulaMinusOneEvent() {
//    return new CuratedFormulaEvent(
//       afterimageInstanceIdentity,
//       new FormulaEvent(
//          formulaCreator,
//          formulaDownvoteIdentifierTag,
//          awardDownvoteDefinitionEvent,
//          MINUS_ONE_FORMULA,
//          new Relay(afterimageRelayUrl)),
//       new ReferenceTag(afterimageRelayUrl),
//       new Relay(afterimageRelayUrl));
//  }
//
//  @Override
//  protected void submitPlusOneFormulaEventToImplSpecificRelay() {
//    submitAimgEvent(plusOneCuratedFormulaEvent);
//  }
//
//  @Override
//  protected void submitDownvoteFormulaEventToImplSpecificRelay() {
//    submitAimgEvent(minusOneCuratedFormulaEvent);
//  }
//}
