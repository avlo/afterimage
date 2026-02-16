package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.config.SingleContainerTestConfig;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.nostr.util.Util;
import com.prosilion.subdivisions.client.reactive.ReactiveNostrRelayClient;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(SingleContainerTestConfig.class)
public class SearchRelaysListIT {
  /**
   * definitionsCreatorIdentity:   02d49b23e02985a760e8bc2f5ee86a3089569806f5f6a670fba3317568d14262
   * <p>
   * voteSubmitterIdentity:        611eda70943b4f67d1674068f5c86cedbdc3438bb41245b129a6311e4f308295
   * <p>
   * voteReceierIdentity:          985a5b9ea911bb8f9d9dca82c03f776d68fdc452b774295a874423a0fa5e8879
   */

  public static final String REPUTATION = "TEST_REPUTATION";
  public static final String AWARD_UNIT_UPVOTE = "TEST_UNIT_UPVOTE";

  public static final String PLUS_ONE_FORMULA = "+1";

  private final IdentifierTag reputationIdentifierTag = new IdentifierTag(REPUTATION);
  private final IdentifierTag upvoteIdentifierTag = new IdentifierTag(AWARD_UNIT_UPVOTE);

  private final Identity definitionsCreatorIdentity = // Identity.generateRandomIdentity();
      Identity.create("bbb4585483196998204846989544737603523651520600328805626488477202");

  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventPlusOneFormula;

  private final Identity voteRecipientIdentity;

  private final String superconductorRelayUrl;
  private final String afterimageRelayUrl;

  @Autowired
  public SearchRelaysListIT(
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl
//      ,
//      @NonNull @Value("${afterimage.relay.url.two}") String afterimageRelayUrl2
  ) throws ParseException, IOException, InterruptedException {
    this.superconductorRelayUrl = superconductorRelayUrl;
    this.afterimageRelayUrl = afterimageRelayUrl;
    Relay superconductorRelay = new Relay(superconductorRelayUrl);

    Identity voteSubmitterIdentity = Identity.create("aaa4585483196998204846989544737603523651520600328805626488477202");
    voteRecipientIdentity = // Identity.generateRandomIdentity();
        Identity.create("ccc4585483196998204846989544737603523651520600328805626488477202");

    BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent = new BadgeDefinitionGenericEvent(
        definitionsCreatorIdentity,
        upvoteIdentifierTag,
        superconductorRelay,
        String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", definitionsCreatorIdentity.getPublicKey()));

    FormulaEvent plusOneFormulaEvent = new FormulaEvent(
        definitionsCreatorIdentity,
        upvoteIdentifierTag,
        superconductorRelay,
        awardUpvoteDefinitionEvent,
        PLUS_ONE_FORMULA);

    badgeDefinitionReputationEventPlusOneFormula = new BadgeDefinitionReputationEvent(
        definitionsCreatorIdentity,
        reputationIdentifierTag,
        superconductorRelay,
        AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
        plusOneFormulaEvent);

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent =
        new BadgeAwardGenericEvent<>(
            voteSubmitterIdentity,
            voteRecipientIdentity.getPublicKey(),
            superconductorRelay,
            badgeDefinitionReputationEventPlusOneFormula,
            String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", voteRecipientIdentity.getPublicKey()));

////    reminder : do lsports for relay2's docker internal url ...
    log.debug("1of4 - sendEventToSuperconductor(awardUpvoteDefinitionEvent)");
    sendEventToSuperconductor(awardUpvoteDefinitionEvent);
//    cacheService.save(awardUpvoteDefinitionEvent);

    log.debug("2of4 - sendEventToSuperconductor(plusOneFormulaEvent)");
////    ... req'd by events' validation / relay  processing calls    
    sendEventToSuperconductor(plusOneFormulaEvent);
//    cacheService.save(plusOneFormulaEvent);

    log.debug("3of4 - sendEventToSuperconductor(badgeDefinitionReputationEventPlusOneFormula)");
////    ... req'd by events' validation / relay  processing calls    
    sendEventToSuperconductor(badgeDefinitionReputationEventPlusOneFormula);
//    cacheService.save(badgeDefinitionReputationEventPlusOneFormula);

    log.debug("4of4 - sendEventToSuperconductor(badgeAwardUpvoteEvent)");
////    ... req'd by events' validation / relay  processing calls    
//    cacheService.save(badgeAwardUpvoteEvent);
//    sendEventToAimg2(badgeAwardUpvoteEvent);
    sendEventToSuperconductor(badgeAwardUpvoteEvent);
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException, InterruptedException {
//  submit search relays list event to aImg w/ SC url, should:
//    1. get upvote event from SC
//    2. create REPUTATION event in aImg
    TestSubscriber<OkMessage> afterImageCreateSearchRelaysListEventsSubscriber = new TestSubscriber<>();
    ReactiveNostrRelayClient afterimageEventMessageRelayClient = new ReactiveNostrRelayClient(afterimageRelayUrl);
    afterimageEventMessageRelayClient
        .send(
            new EventMessage(
                createSearchRelaysListEventMessage(superconductorRelayUrl)),
            afterImageCreateSearchRelaysListEventsSubscriber);
    List<OkMessage> okItems = afterImageCreateSearchRelaysListEventsSubscriber.getItems();
//    TimeUnit.MILLISECONDS.sleep(2000);
//    afterimageEventMessageRelayClient.closeSocket();
    Boolean flag = okItems.getFirst().getFlag();
    assertEquals(true, flag);

//    new AfterimageMeshRelayService(afterimageRelayUri)
//        .send(
//            new EventMessage(
//                createSearchRelaysListEventMessage(superconductorRelayUri_2)),
//            new TestSubscriber<>());
//
//    TimeUnit.MILLISECONDS.sleep(1000);

//    query Aimg for above REPUTATION event
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_A = new TestSubscriber<>();
    ReactiveNostrRelayClient afterimageRepRequestClient = new ReactiveNostrRelayClient(afterimageRelayUrl);
    afterimageRepRequestClient.send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            voteRecipientIdentity.getPublicKey(),
            definitionsCreatorIdentity.getPublicKey()),
        afterImageEventsSubscriber_A);

//    TimeUnit.MILLISECONDS.sleep(2000);
//    afterimageRepRequestClient.closeSocket();

    log.debug("afterimage returned superconductor events:");
    List<BaseMessage> items_3 = afterImageEventsSubscriber_A.getItems();
    List<EventIF> returnedReqGenericEvents_2 = getGenericEvents(items_3);

    assertEquals("1", returnedReqGenericEvents_2.getFirst().getContent());
//    assertEquals(returnedReqGenericEvents_2.getFirst().getPublicKey().toHexString(), definitionsCreatorIdentity.getPublicKey().toHexString());
//    assertEquals(Kind.BADGE_AWARD_EVENT, returnedReqGenericEvents_2.getFirst().getKind());
    log.debug("nuthin");
////    more SC events
//    BadgeAwardUpvoteEvent event_2 = new BadgeAwardUpvoteEvent(
//        authorIdentity,
//        upvotedUser.getPublicKey(),
//        badgeDefinitionUpvoteEvent);
//
//    assertEquals(event_2.getPublicKey().toHexString(), authorIdentity.getPublicKey().toHexString());
//
////  submit upvote event to SC
//    TestSubscriber<OkMessage> okMessageSubscriber_sc_1_2 = new TestSubscriber<>();
//    new AfterimageMeshRelayService(superconductorRelayUri).send(new EventMessage(event_2), okMessageSubscriber_sc_1_2);
//
//    TestSubscriber<OkMessage> okMessageSubscriber_sc_1_3 = new TestSubscriber<>();
//    new AfterimageMeshRelayService(superconductorRelayUri).send(new EventMessage(event_2), okMessageSubscriber_sc_1_3);
//
//    TimeUnit.MILLISECONDS.sleep(1000);
//
//    List<OkMessage> items_4 = okMessageSubscriber_sc_1_2.getItems();
//    assertEquals(true, items_4.getFirst().getFlag());
//
//    TimeUnit.MILLISECONDS.sleep(100);
//
//    List<OkMessage> items_5 = okMessageSubscriber_sc_1_3.getItems();
//    assertEquals(true, items_5.getFirst().getFlag());
//
//    TimeUnit.MILLISECONDS.sleep(100);
//
//    log.debug("afterimage returned superconductor events:");
//    List<BaseMessage> items_6 = afterImageEventsSubscriber_A.getItems();
//    log.debug("  {}", items_6);
//
//    TestSubscriber<BaseMessage> afterImageEventsSubscriber_B = new TestSubscriber<>();
//    final AfterimageMeshRelayService afterimageRepRequestClient_2 = new AfterimageMeshRelayService(afterimageRelayUri);
//    afterimageRepRequestClient_2.send(
//        createAfterImageReqMessage(Factory.generateRandomHex64String(), upvotedUser.getPublicKey()),
//        afterImageEventsSubscriber_B);
//
//    List<BaseMessage> items_7 = afterImageEventsSubscriber_B.getItems();
//    log.debug("  {}", items_7);
//
//    List<EventIF> returnedReqGenericEvents_3 = getGenericEvents(items_7);
//    assertEquals("3", returnedReqGenericEvents_3.getFirst().getContent());
//
//    TestSubscriber<OkMessage> okMessageSubscriber_sc_2_2 = new TestSubscriber<>();
//    new AfterimageMeshRelayService(superconductorRelayUri_2).send(new EventMessage(event_2), okMessageSubscriber_sc_2_2);
//
//    TestSubscriber<OkMessage> okMessageSubscriber_sc_2_3 = new TestSubscriber<>();
//    new AfterimageMeshRelayService(superconductorRelayUri_2).send(new EventMessage(event_2), okMessageSubscriber_sc_2_3);
//
//    TestSubscriber<BaseMessage> afterImageEventsSubscriber_9 = new TestSubscriber<>();
//    final AfterimageMeshRelayService afterimageRepRequestClient_3 = new AfterimageMeshRelayService(afterimageRelayUri);
//    afterimageRepRequestClient_3.send(
//        createAfterImageReqMessage(Factory.generateRandomHex64String(), upvotedUser.getPublicKey()),
//        afterImageEventsSubscriber_9);
//
//    List<BaseMessage> items_8 = afterImageEventsSubscriber_9.getItems();
//    log.debug("  {}", items_8);
//
//    List<EventIF> returnedReqGenericEvents_4 = getGenericEvents(items_8);
//    assertEquals("3", returnedReqGenericEvents_4.getFirst().getContent());
//
//    afterimageRepRequestClient_3.closeSocket();
  }

  private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
  }

  private ReqMessage createAfterImageReqMessage(
      String subscriberId,
      PublicKey upvotedUserPublicKey,
      PublicKey badgeCreatorPublicKey) throws JsonProcessingException {
    System.out.println("333333333333333333333");
    System.out.println("333333333333333333333");
//    ExternalIdentityTagFilter externalIdentityTagFilter = new ExternalIdentityTagFilter(BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG);
    ReqMessage reqMessageWithStuff = new ReqMessage(
        subscriberId,
        new Filters(
            new KindFilter(
                Kind.BADGE_AWARD_EVENT),
//            new IdentifierTagFilter(reputationIdentifierTag),
            new AddressTagFilter(
                new AddressTag(
                    Kind.BADGE_DEFINITION_EVENT,
                    badgeCreatorPublicKey,
                    reputationIdentifierTag)),
            new ReferencedPublicKeyFilter(
                new PubKeyTag(
                    upvotedUserPublicKey))
//            ,
//            externalIdentityTagFilter
        ));

    ReqMessage reqMessage = reqMessageWithStuff;
    log.debug(Util.prettyFormatJson(reqMessage.encode()));
    System.out.println("333333333333333333333");
    System.out.println("333333333333333333333");
    return reqMessage;
  }

  private BaseEvent createSearchRelaysListEventMessage(String url) {
    Identity searchRelaysListEventSubmitterIdentity = Identity.generateRandomIdentity();
    log.debug("\n\n\nSearch Relays List sent from aImg IT 5556...");
    SearchRelaysListEvent searchRelaysListEvent = new SearchRelaysListEvent(
        searchRelaysListEventSubmitterIdentity,
        new RelaysTag(new Relay(url)),
        "Search Relays List sent from aImg IT 5556");
    return searchRelaysListEvent;
  }

  private void sendEventToSuperconductor(BaseEvent baseEvent) throws IOException, InterruptedException {
    final String RED_BOLD_BRIGHT = "\033[1;91m";
    final String GREEN_BOLD = "\033[1;32m";
    final String RESET = "\033[0m";
    String greenFont = GREEN_BOLD + "%s" + RESET;
    String redFont = RED_BOLD_BRIGHT + "%s" + RESET;

    final TestSubscriber<OkMessage> subscriber = new TestSubscriber<>();
    ReactiveNostrRelayClient superconductorRelayClient = new ReactiveNostrRelayClient(superconductorRelayUrl);
    superconductorRelayClient.send(
        new EventMessage(baseEvent),
        subscriber);
    List<OkMessage> scReturnedOkMessage = subscriber.getItems();
    Boolean flag = scReturnedOkMessage.getFirst().getFlag();
    log.debug("\n  ***********  OKMessage received from sendEventToAimgRelay2? [{}] ************\n",
        String.format(flag ? greenFont : redFont, flag.toString().toUpperCase()));
//    assertEquals(true, upvoteDefinitionOkMessageItems.getFirst().getFlag());
    subscriber.dispose();
    superconductorRelayClient.closeSocket();
//    new AfterimageMeshRelayService(afterimageRelayUrl2).closeSocket();
  }
}
