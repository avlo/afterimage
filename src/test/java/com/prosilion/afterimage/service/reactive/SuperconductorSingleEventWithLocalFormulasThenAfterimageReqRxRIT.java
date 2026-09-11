//package com.prosilion.afterimage.service.reactive;
//
//import com.prosilion.afterimage.config.SingleContainerTestConfig;
//import com.prosilion.afterimage.enums.AfterimageKindType;
//import com.prosilion.nostr.NostrException;
//import com.prosilion.nostr.event.BadgeAwardGenericEvent;
//import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
//import com.prosilion.nostr.event.BaseEvent;
//import com.prosilion.nostr.event.EventIF;
//import com.prosilion.nostr.event.FormulaEvent;
//import com.prosilion.nostr.event.GenericEventRecord;
//import com.prosilion.nostr.event.curated.BadgeDefinitionReputationEvent;
//import com.prosilion.nostr.event.curated.CuratedFormulaEvent;
//import com.prosilion.nostr.event.internal.Relay;
//import com.prosilion.nostr.filter.Filters;
//import com.prosilion.nostr.message.BaseMessage;
//import com.prosilion.nostr.tag.EventTag;
//import com.prosilion.nostr.tag.PubKeyTag;
//import com.prosilion.nostr.tag.ReferenceTag;
//import com.prosilion.nostr.user.Identity;
//import com.prosilion.nostr.user.PublicKey;
//import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
//import com.prosilion.superconductor.base.cache.curated.CacheCuratedFormulaEventServiceIF;
//import java.util.List;
//import java.util.Optional;
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
//import org.springframework.test.context.TestPropertySource;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@Slf4j
//@TestMethodOrder(MethodOrderer.MethodName.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
//@ActiveProfiles("test")
//@Import(SingleContainerTestConfig.class)
//@TestPropertySource(properties = {
//   "superconductor.event.curation.active=true"
//})
//public class SuperconductorSingleEventWithLocalFormulasThenAfterimageReqIT extends AbstractRxRIT {
//  private final CacheCuratedFormulaEventServiceIF cacheCuratedFormulaEventServiceIF;
//  private final BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent;
//
//  @Autowired
//  public SuperconductorSingleEventWithLocalFormulasThenAfterimageReqIT(
//     @NonNull Identity afterimageInstanceIdentity,
//     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
//     @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl,
//     @NonNull CacheCuratedFormulaEventServiceIF cacheCuratedFormulaEventServiceIF) {
//    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrl);
//    this.cacheCuratedFormulaEventServiceIF = cacheCuratedFormulaEventServiceIF;
//
//    this.awardUpvoteDefinitionEvent =
//       new BadgeDefinitionGenericEvent(
//          upvoteAndOrDownvoteDefnCreator,
//          upvoteIdentifierTag,
//          String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteAndOrDownvoteDefnCreator.getPublicKey()),
//          superconductorRelay);
//
//    submitAimgEvent(
//       createBadgeDefinitionReputationEvent());
//  }
//
//  private BadgeDefinitionReputationEvent createBadgeDefinitionReputationEvent() {
//    List<CuratedFormulaEvent> curatedFormulaEvents = formulaEventList.stream()
//       .map(EventIF::getId)
//       .map(eventId ->
//          cacheCuratedFormulaEventServiceIF.getByDirect(
//             new EventTag(eventId)))
//       .flatMap(Optional::stream).toList();
//
//    BadgeDefinitionReputationEvent badgeDefinitionReputationEvent = new BadgeDefinitionReputationEvent(
//       repDefnCreator,
//       afterimageInstanceIdentity.getPublicKey(),
//       reputationIdentifierTag,
//       AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
//       new Relay(afterimageRelayUrl),
//       curatedFormulaEvents);
//
//    return badgeDefinitionReputationEvent;
//  }
//
//  @Test
//  void aSuperconductorEventThenAfterimageReq() throws NostrException {
//    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> upvoteEvent =
//       new BadgeAwardGenericEvent<>(
//          submitter,
//          recipient.getPublicKey(),
//          awardUpvoteDefinitionEvent,
//          superconductorRelay);
//
//    EventIF simulateIncomingUpvoteEvent = submitSCEvent(
//       upvoteEvent,
//       superconductorRelayUrl);
//
//    submitRelayEventWithDuration_backup(simulateIncomingUpvoteEvent, afterimageRelayUrl);
//
//    assertEquals(
//       "1",
//       submitAfterImageReq(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl).getFirst().getContent());
//  }
//
//  @Test
//  void bRepeatRecipientSuperconductorEventThenAfterimageReq() throws NostrException {
//    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> upvoteEvent =
//       new BadgeAwardGenericEvent<>(
//          submitter,
//          recipient.getPublicKey(),
//          awardUpvoteDefinitionEvent,
//          superconductorRelay);
//    
//    EventIF simulateIncomingUpvoteEvent = submitSCEvent(
//       upvoteEvent,
//       superconductorRelayUrl);
//
//    submitRelayEventWithDuration_backup(simulateIncomingUpvoteEvent, afterimageRelayUrl);
//    
//    assertEquals(
//       "2",
//       submitAfterImageReq(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl).getFirst().getContent());
//  }
//
////  @Test
//  void dRepeatRecipientSuperconductorEventThenAfterimageReq() throws NostrException {
//    BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent =
//       new BadgeDefinitionGenericEvent(
//          upvoteAndOrDownvoteDefnCreator,
//          upvoteIdentifierTag,
//          String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteAndOrDownvoteDefnCreator.getPublicKey()),
//          superconductorRelay);
//
//    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> upvoteEvent =
//       new BadgeAwardGenericEvent<>(
//          submitter,
//          recipient.getPublicKey(),
//          awardUpvoteDefinitionEvent,
//          superconductorRelay);
//
//    EventIF simulateIncomingUpvoteEvent = submitSCEvent(
//       upvoteEvent,
//       superconductorRelayUrl);
//
//    submitRelayEventWithDuration_backup(simulateIncomingUpvoteEvent, afterimageRelayUrl);
//
//    assertEquals(
//       "3",
//       submitAfterImageReq(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl).getFirst().getContent());
//  }
//
//  //  @Test
//  void bSuperconductorEventEmptyAddressTagRelayTriesSourceRelayThenAfterimageReq() throws NostrException {
//    BadgeDefinitionGenericEvent awardUpvoteDefinitionEventNullRelay =
//       new BadgeDefinitionGenericEvent(
//          upvoteAndOrDownvoteDefnCreator,
//          upvoteIdentifierTag);
//
////    submitSCEventCheckEventTagEventId(
////       awardUpvoteDefinitionEventNullRelay,
////       superconductorRelayUrl,
////       new Filters(
////          new AuthorFilter(upvoteAndOrDownvoteDefnCreator.getPublicKey()),
////          new KindFilter(Kind.BADGE_DEFINITION_EVENT)),
////       upvoteAndOrDownvoteDefnCreator.getPublicKey());
//
//    PublicKey newRecipient = Identity.generateRandomIdentity().getPublicKey();
//    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> upvoteEvent =
//       new BadgeAwardGenericEvent<>(
//          submitter,
//          newRecipient,
//          awardUpvoteDefinitionEventNullRelay,
//          superconductorRelay);
//
//    EventIF submitSCEvent = submitSCEvent(upvoteEvent, superconductorRelayUrl);
//
//    submitAimgEventWithDuration_backup(submitSCEvent);
//
//    assertEquals(
//       "1",
//       submitAfterImageReq(new PubKeyTag(newRecipient), afterimageRelayUrl).getFirst().getContent());
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
//    List<EventIF> receivedEventIFs = getEventIFs(baseMessages);
//    receivedEventIFs.stream().map(EventIF::asGenericEventRecord).map(GenericEventRecord::createPrettyPrintJson).forEach(log::debug);
//
//    EventIF upvoteEventIF = receivedEventIFs.getFirst();
//
//    assertEquals(event.getId(), upvoteEventIF.requireFirstTag(EventTag.class).getEventId());
//    assertEquals(publicKey, upvoteEventIF.getPublicKey());
//    assertEquals(upvoteEventIF.getContent(), event.getContent());
////    assertEquals(upvoteEventIF.getPublicKey().toHexString(), event.getPublicKey().toHexString());
//    assertEquals(upvoteEventIF.getKind(), event.getKind());
//
//    return upvoteEventIF;
//  }
//
//  @Override
//  protected List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> createBadgeAwardGenericEventList() {
//    return List.of(
//       createUpvoteEvent(new Relay(afterimageRelayUrl)),
//       createDownvoteEvent(new Relay(afterimageRelayUrl)));
//  }
//
//  protected BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> createUpvoteEvent(Relay relay) {
//    return new BadgeAwardGenericEvent<>(
//       submitter,
//       recipient.getPublicKey(),
//       createBadgeAwardUpvoteDefinitionEvent(),
//       relay);
//  }
//
//  protected BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> createDownvoteEvent(Relay relay) {
//    return new BadgeAwardGenericEvent<>(
//       submitter,
//       recipient.getPublicKey(),
//       createBadgeAwardDownvoteDefinitionEvent(),
//       relay);
//  }
//
//  protected FormulaEvent createPlusOneFormulaEvent() {
//    return new FormulaEvent(
//       formulaCreator,
//       formulaUpvoteIdentifierTag,
//       createBadgeAwardUpvoteDefinitionEvent(),
//       PLUS_ONE_FORMULA,
//       new Relay(afterimageRelayUrl));
//  }
//
//  protected FormulaEvent createMinusOneFormulaEvent() {
//    return new FormulaEvent(
//       formulaCreator,
//       formulaDownvoteIdentifierTag,
//       createBadgeAwardDownvoteDefinitionEvent(),
//       MINUS_ONE_FORMULA,
//       new Relay(afterimageRelayUrl));
//  }
//
//  protected CuratedFormulaEvent createCuratedFormulaPlusOneEvent() {
//    return new CuratedFormulaEvent(
//       afterimageInstanceIdentity,
//       new FormulaEvent(
//          formulaCreator,
//          formulaUpvoteIdentifierTag,
//          createBadgeAwardUpvoteDefinitionEvent(),
//          PLUS_ONE_FORMULA,
//          new Relay(afterimageRelayUrl)),
//       new ReferenceTag(afterimageRelayUrl),
//       new Relay(afterimageRelayUrl));
//  }
//
//  protected CuratedFormulaEvent createCuratedFormulaMinusOneEvent() {
//    return new CuratedFormulaEvent(
//       afterimageInstanceIdentity,
//       new FormulaEvent(
//          formulaCreator,
//          formulaDownvoteIdentifierTag,
//          createBadgeAwardDownvoteDefinitionEvent(),
//          MINUS_ONE_FORMULA,
//          new Relay(afterimageRelayUrl)),
//       new ReferenceTag(afterimageRelayUrl),
//       new Relay(afterimageRelayUrl));
//  }
//
//  protected BadgeDefinitionGenericEvent createBadgeAwardUpvoteDefinitionEvent() {
//    return new BadgeDefinitionGenericEvent(
//       upvoteAndOrDownvoteDefnCreator,
//       upvoteIdentifierTag,
//       String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteAndOrDownvoteDefnCreator.getPublicKey()),
//       superconductorRelay);
//  }
//
//  protected BadgeDefinitionGenericEvent createBadgeAwardDownvoteDefinitionEvent() {
//    return new BadgeDefinitionGenericEvent(
//       upvoteAndOrDownvoteDefnCreator,
//       downvoteIdentifierTag,
//       String.format("awardDownvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteAndOrDownvoteDefnCreator.getPublicKey()),
//       superconductorRelay);
//  }
//
//  @Override
//  protected List<BadgeDefinitionGenericEvent> createBadgeDefinitionGenericEventList() {
//    return List.of(
//       createBadgeAwardUpvoteDefinitionEvent()
////       ,
////       createBadgeAwardDownvoteDefinitionEvent()
//    );
//  }
//
//  @Override
//  protected List<FormulaEvent> createFormulaEventList() {
//    return List.of(
//       createPlusOneFormulaEvent()
////       ,
////       createMinusOneFormulaEvent()
//    );
//  }
//
////  @Override
////  protected List<CuratedFormulaEvent> createCuratedFormulaEventList() {
////    return List.of(
////       createCuratedFormulaPlusOneEvent()
////       ,
////       createCuratedFormulaMinusOneEvent()
////    );
////  }
//}
