package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.TestcontainersConfig;
import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericVoteEvent;
import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.EventService;
import com.prosilion.superconductor.base.service.event.type.EventPluginIF;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
//@Import(value = {AfterimageWsConfig.class, TestcontainersConfig.class})
public class SuperconductorEventThenAfterimageReqIT {
  private final EventService eventService;

  public static final String REPUTATION = "TEST_REPUTATION";
  public static final String UNIT_UPVOTE = "TEST_UNIT_UPVOTE";
  public static final String PLUS_ONE_FORMULA = "+1";

  public final IdentifierTag reputationIdentifierTag = new IdentifierTag(REPUTATION);
  public final IdentifierTag upvoteIdentifierTag = new IdentifierTag(UNIT_UPVOTE);

  public final Identity identity = Identity.generateRandomIdentity();

  public static final String PLATFORM = FollowSetsIT.class.getPackageName();
  public static final String IDENTITY = FollowSetsIT.class.getSimpleName();
  public static final String PROOF = String.valueOf(FollowSetsIT.class.hashCode());

  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventPlusOneFormula;

  Identity afterimageInstanceIdentity;

  private final String superconductorRelayUri;
  private final String afterimageRelayUri;

  @Autowired
  public SuperconductorEventThenAfterimageReqIT(
      @NonNull @Qualifier("eventService") EventService eventService,
      @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUri,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri,
      @NonNull Identity afterimageInstanceIdentity) throws ParseException {
    Relay relay = new Relay(afterimageRelayUri);

    BadgeDefinitionAwardEvent awardUpvoteDefinitionEvent = new BadgeDefinitionAwardEvent(identity, upvoteIdentifierTag, relay);
    FormulaEvent plusOneFormulaEvent = new FormulaEvent(identity, upvoteIdentifierTag, relay, awardUpvoteDefinitionEvent, PLUS_ONE_FORMULA);

    badgeDefinitionReputationEventPlusOneFormula = new BadgeDefinitionReputationEvent(
        identity,
        reputationIdentifierTag,
        relay,
        new ExternalIdentityTag(PLATFORM, IDENTITY, PROOF),
        plusOneFormulaEvent);

    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.eventService = eventService;
    this.superconductorRelayUri = superconductorRelayUri;
    this.afterimageRelayUri = afterimageRelayUri;

    eventPlugin.processIncomingEvent(awardUpvoteDefinitionEvent);
    eventPlugin.processIncomingEvent(plusOneFormulaEvent);
    eventPlugin.processIncomingEvent(badgeDefinitionReputationEventPlusOneFormula);
//    cacheService.save(badgeAwardUpvoteEvent);
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException, InterruptedException {
    final AfterimageMeshRelayService afterimageSubscriberCheckClient = new AfterimageMeshRelayService(afterimageRelayUri);
    final Identity upvotedUser = Identity.generateRandomIdentity();

    TestSubscriber<BaseMessage> reputationRequestSubscriberCheck = new TestSubscriber<>();
    afterimageSubscriberCheckClient.send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            upvotedUser.getPublicKey()),
        reputationRequestSubscriberCheck);

    TimeUnit.MILLISECONDS.sleep(1000);
    //  test initial aImg events state, should have zero reputation events for upvotedUser

    log.debug("AAAAAAAAAAAAAA");
    log.debug("AAAAAAAAAAAAAA");
    log.debug("afterimage initial events:");
    List<BaseMessage> initialItems = reputationRequestSubscriberCheck.getItems();
    log.debug("BBBBBBBBBBBBBBB");
    log.debug("BBBBBBBBBBBBBBB");
    log.debug("  {}", initialItems);

    List<EventIF> initialEvents = getGenericEvents(initialItems);
    log.debug("CCCCCCCCCCCCCCC");
    log.debug("CCCCCCCCCCCCCCC");
    assertEquals(0, initialEvents.size());
    log.debug("ccccccccccccccc");
    log.debug("ccccccccccccccc");
//    afterimageSubscriberCheckClient.closeSocket();

//  begin SC votes submissions  

    final Identity authorIdentity = Identity.generateRandomIdentity();
    AfterimageMeshRelayService superconductorRelayReactiveClient = new AfterimageMeshRelayService(superconductorRelayUri);
    log.debug("ddddddddddddddd");
    log.debug("ddddddddddddddd");
    log.debug(superconductorRelayReactiveClient.toString());

    BadgeAwardGenericVoteEvent badgeAwardUpvoteEvent = new BadgeAwardGenericVoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        badgeDefinitionReputationEventPlusOneFormula);
    log.debug("XXXXXXXXXXXXXXXX" + badgeAwardUpvoteEvent);
    log.debug("XXXXXXXXXXXXXXXX badgeAwardUpvoteEvent: " + badgeAwardUpvoteEvent);
    log.debug("XXXXXXXXXXXXXXXX" + badgeAwardUpvoteEvent);

    log.debug("eeeeeeeeeeeeeee");
    log.debug("eeeeeeeeeeeeeee");

//    GenericEventKindTypeIF badgeAwardUpvoteEvent_1 =
//        new GenericDocumentKindTypeDto(
//            badgeAwardUpvoteEvent,
//            SuperconductorKindType.UNIT_UPVOTE)
//            .convertBaseEventToGenericEventKindTypeIF();

    assertEquals(badgeAwardUpvoteEvent.getPublicKey().toHexString(), authorIdentity.getPublicKey().toHexString());

//    submit Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber_1 = new TestSubscriber<>();
    log.debug("fffffffffffffff");
    log.debug("fffffffffffffff");
    superconductorRelayReactiveClient.send(new EventMessage(badgeAwardUpvoteEvent), okMessageSubscriber_1);
    log.debug("DDDDDDDDDDDDDDD");
    log.debug("DDDDDDDDDDDDDDD");

    TimeUnit.MILLISECONDS.sleep(2500);

    List<OkMessage> items_1 = okMessageSubscriber_1.getItems();
    log.debug("EEEEEEEEEEEEEEE");
    log.debug("EEEEEEEEEEEEEEE");
    assertEquals(true, items_1.getFirst().getFlag());

    //    submit Req for above badgeAwardUpvoteEvent to superconductor

    TestSubscriber<BaseMessage> superconductorEventsSubscriber_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(Factory.generateRandomHex64String()),
        superconductorEventsSubscriber_1);

    TimeUnit.MILLISECONDS.sleep(2500);

    log.debug("00000000000000");
    log.debug("00000000000000");
    log.debug("retrieved afterimage events:");
    List<BaseMessage> superconductorEventsSubscriber_1Items = superconductorEventsSubscriber_1.getItems();
    List<EventIF> returnedSuperconductorEvents = getGenericEvents(superconductorEventsSubscriber_1Items);

    assertEquals(returnedSuperconductorEvents.getFirst().getId(), badgeAwardUpvoteEvent.getId());
    assertEquals(returnedSuperconductorEvents.getFirst().getContent(), badgeAwardUpvoteEvent.getContent());
    assertEquals(returnedSuperconductorEvents.getFirst().getPublicKey().toHexString(), badgeAwardUpvoteEvent.getPublicKey().toHexString());
    assertEquals(returnedSuperconductorEvents.getFirst().getKind(), badgeAwardUpvoteEvent.getKind());

//    save SC result to Aimg
    log.debug("11111111111111");
    log.debug("11111111111111");
    log.debug("save SC result to Aimg:");
    returnedSuperconductorEvents.forEach(gev ->
        eventService.processIncomingEvent(
            new EventMessage(createGenericEventRecord(gev))));

    TimeUnit.MILLISECONDS.sleep(1000);

//    query Aimg for above badgeAwardUpvoteEvent
    log.debug("22222222222222");
    log.debug("22222222222222");
    log.debug("query Aimg for above badgeAwardUpvoteEvent:");
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_A = new TestSubscriber<>();
    final AfterimageMeshRelayService afterimageRepRequestClient = new AfterimageMeshRelayService(afterimageRelayUri);
    afterimageRepRequestClient.send(
        createAfterImageReqMessage(Factory.generateRandomHex64String(), upvotedUser.getPublicKey()),
        afterImageEventsSubscriber_A);

    TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("33333333333333");
    log.debug("33333333333333");
    log.debug("afterimage returned superconductor events:");
    List<BaseMessage> items_2 = afterImageEventsSubscriber_A.getItems();
    log.debug("  {}", items_2);

    List<EventIF> returnedReqGenericEvents_2 = getGenericEvents(items_2);

    assertEquals("1", returnedReqGenericEvents_2.getFirst().getContent());
    assertEquals(returnedReqGenericEvents_2.getFirst().getPublicKey().toHexString(), afterimageInstanceIdentity.getPublicKey().toHexString());
    assertEquals(returnedReqGenericEvents_2.getFirst().getKind(), badgeAwardUpvoteEvent.getKind());

    final AfterimageMeshRelayService afterimageMeshRelayService = new AfterimageMeshRelayService(afterimageRelayUri);

    //    create & submit subscriber's first Event to superconductor
    BadgeAwardGenericVoteEvent upvote_1 = new BadgeAwardGenericVoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        badgeDefinitionReputationEventPlusOneFormula);
//    GenericEventKindTypeIF upvote_1 = new GenericDocumentKindTypeDto(
//        upvote_1,
//        SuperconductorKindType.UNIT_UPVOTE).convertBaseEventToGenericEventKindTypeIF();

    TestSubscriber<OkMessage> okMessageSubscriber_upvote_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(upvote_1), okMessageSubscriber_upvote_1);
    TimeUnit.MILLISECONDS.sleep(1000);

    List<OkMessage> items1 = okMessageSubscriber_upvote_1.getItems();
    TimeUnit.MILLISECONDS.sleep(50);

    assertEquals(true, items1.getFirst().getFlag());
    log.debug("received 1of2 OkMessage...");

//    create & submit subscriber's second Event to superconductor
    BadgeAwardGenericVoteEvent upvote_2 = new BadgeAwardGenericVoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        badgeDefinitionReputationEventPlusOneFormula);

    //    GenericEventKindTypeIF upvote_2 = new GenericDocumentKindTypeDto(
//        upvote_2,
//        SuperconductorKindType.UNIT_UPVOTE).convertBaseEventToGenericEventKindTypeIF();

//    okMessageSubscriber_1.dispose();
    TestSubscriber<OkMessage> okMessageSubscriber_2 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(upvote_2), okMessageSubscriber_2);
    TimeUnit.MILLISECONDS.sleep(1000);

    List<OkMessage> items = okMessageSubscriber_2.getItems();
    assertEquals(true, items.getFirst().getFlag());
    log.debug("received 2of2 OkMessage...");

////    create & submit subscriber's third Event to superconductor
//    BadgeAwardGenericVoteEvent textNoteEvent_3 = new BadgeAwardGenericVoteEvent(authorIdentity, upvotedUser.getPublicKey(), badgeDefinitionUpvoteEvent);
//    GenericEventKindTypeIF genericEventKindIF3 = new GenericDocumentKindTypeDto(textNoteEvent_3, SuperconductorKindType.UNIT_UPVOTE).convertBaseEventToGenericEventKindTypeIF();
//
////    okMessageSubscriber_1.dispose();
//    TestSubscriber<OkMessage> okMessageSubscriber_3 = new TestSubscriber<>();
//    superconductorRelayReactiveClient.send(new EventMessage(genericEventKindIF3), okMessageSubscriber_3);
//    TimeUnit.MILLISECONDS.sleep(50);
//
//    List<OkMessage> items3 = okMessageSubscriber_2.getItems();
//    assertEquals(true, items3.getFirst().getFlag());
//    log.debug("received 2of3 OkMessage...");

// # --------------------- REQ -------------------    
//    submit votes Req to superconductor

    TestSubscriber<BaseMessage> superConductorEventsSubscriber_W = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(Factory.generateRandomHex64String()), superConductorEventsSubscriber_W);

    TimeUnit.MILLISECONDS.sleep(1000);

    List<EventIF> returnedVotesFromSc = getGenericEvents(
        superConductorEventsSubscriber_W.getItems());

    assertTrue(returnedVotesFromSc.stream().map(EventIF::getId).anyMatch(upvote_1.getId()::equals));
    assertTrue(returnedVotesFromSc.stream().map(EventIF::getId).anyMatch(upvote_2.getId()::equals));
    assertTrue(returnedVotesFromSc.stream().map(EventIF::getPublicKey).map(PublicKey::toString).anyMatch(upvote_1.getPublicKey().toString()::equals));
    assertTrue(returnedVotesFromSc.stream().map(EventIF::getPublicKey).map(PublicKey::toString).anyMatch(upvote_2.getPublicKey().toString()::equals));
    assertTrue(returnedVotesFromSc.stream().map(EventIF::getKind).anyMatch(upvote_1.getKind()::equals));

//    save SC result to Aimg
    returnedVotesFromSc.forEach(event -> eventService.processIncomingEvent(new EventMessage(createGenericEventRecord(event))));

    TimeUnit.MILLISECONDS.sleep(1000);

//    query Aimg for (as yet to be impl'd) reputation score event
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_V = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        createAfterImageReqMessage(Factory.generateRandomHex64String(), upvotedUser.getPublicKey()), afterImageEventsSubscriber_V);

    TimeUnit.MILLISECONDS.sleep(1000);

    List<EventIF> returnedAfterImageEvents = getGenericEvents(
        afterImageEventsSubscriber_V.getItems());

    TimeUnit.MILLISECONDS.sleep(1000);

//    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getId().equals(textNoteEvent_1.getId())));
    assertEquals(1, returnedAfterImageEvents.size());
    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getPublicKey().toHexString().equals(afterimageInstanceIdentity.getPublicKey().toHexString())));
    assertEquals(returnedAfterImageEvents.getFirst().getKind(), upvote_1.getKind());
    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getKind().equals(upvote_1.getKind())));

    log.debug("returnedAfterImageEvents.size() {}", returnedAfterImageEvents.size());
    log.debug("------");
    log.debug("returnedAfterImageEvents:");
    returnedAfterImageEvents.forEach(a -> log.debug("{}\n----------\n", a.getContent()));
    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals("2")));

//    superconductorRelayReactiveClient.closeSocket();
//    afterimageMeshRelayService.closeSocket();
  }

  private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
  }

  private ReqMessage createAfterImageReqMessage(String subscriberId, PublicKey upvotedUserPublicKey) {
    return new ReqMessage(
        subscriberId,
        new Filters(
            new KindFilter(
                Kind.BADGE_AWARD_EVENT),
            new ReferencedPublicKeyFilter(
                new PubKeyTag(
                    upvotedUserPublicKey)),
            new IdentifierTagFilter(
                badgeDefinitionReputationEventPlusOneFormula.getIdentifierTag())));
  }

  private ReqMessage createSuperconductorReqMessage(String subscriberId) {
    return new ReqMessage(subscriberId,
        new Filters(
//            new ReferencedPublicKeyFilter(new PubKeyTag(upvotedUserPublicKey)),
            new KindFilter(Kind.BADGE_AWARD_EVENT)));
  }

  private GenericEventRecord createGenericEventRecord(EventIF event) {
    return new GenericEventRecord(
        event.getId(),
        event.getPublicKey(),
        event.getCreatedAt(),
        event.getKind(),
        event.getTags(),
        event.getContent(),
        event.getSignature());
  }
}
