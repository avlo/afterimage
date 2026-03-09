package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.config.MultiContainerTestConfig;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.util.Factory;
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
import com.prosilion.superconductor.base.util.RequestSubscriber;
import com.prosilion.superconductor.base.util.SingleReqSubscriptionManager;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.shaded.org.awaitility.core.DurationFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(MultiContainerTestConfig.class)
public class SearchRelaysListRelaySetsIT {
  /**
   * definitionsCreatorIdentity:   02d49b23e02985a760e8bc2f5ee86a3089569806f5f6a670fba3317568d14262
   * <p>
   * voteSubmitterIdentity:        611eda70943b4f67d1674068f5c86cedbdc3438bb41245b129a6311e4f308295
   * <p>
   * voteReceierIdentity:          985a5b9ea911bb8f9d9dca82c03f776d68fdc452b774295a874423a0fa5e8879
   */

  public static final String REPUTATION = "TEST_REPUTATION";
  public static final String AWARD_UNIT_UPVOTE = "TEST_UNIT_UPVOTE";
  public static final String FORMULA_UNIT_UPVOTE = "FORMULA_UNIT_UPVOTE";

  public static final String PLUS_ONE_FORMULA = "+1";

  private final IdentifierTag reputationIdentifierTag = new IdentifierTag(REPUTATION);
  private final IdentifierTag upvoteIdentifierTag = new IdentifierTag(AWARD_UNIT_UPVOTE);
  private final IdentifierTag formulaIdentifierTag = new IdentifierTag(FORMULA_UNIT_UPVOTE);

  private final Identity definitionsCreatorIdentity = // Identity.generateRandomIdentity();
      Identity.create("bbb4585483196998204846989544737603523651520600328805626488477202");

  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventPlusOneFormula;
  private final Duration requestTimeoutDuration;

  private final Identity voteRecipientIdentity;

  private final Relay superconductorRelayReplaced0000;
  private final ReactiveNostrRelayClient superconductorRelayClient;

  private final String afterimageRelayUrl;
  private final Relay afterimageRelayUrl0000;
  private final ReactiveNostrRelayClient afterimageRelayClient;

  BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent;

  @Autowired
  public SearchRelaysListRelaySetsIT(
//      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl,
      @NonNull @Value("${afterimage.relay.url.two}") String afterimageRelayUrlTwo,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
      @NonNull Duration requestTimeoutDuration) throws ParseException, InterruptedException {
    this.requestTimeoutDuration = requestTimeoutDuration;

//    this.superconductorRelayReplaced0000 = new Relay(superconductorRelayUrl.replace("localhost", "0.0.0.0"));
    this.superconductorRelayReplaced0000 = new Relay(superconductorRelayUrl.replace("localhost", "superconductor-afterimage"));

    this.superconductorRelayClient = new ReactiveNostrRelayClient(
//        superconductorRelayUrl.replace("localhost", "0.0.0.0")
//        superconductorRelayReplaced0000.getUrl()
        superconductorRelayUrl
    );

    this.afterimageRelayUrl = afterimageRelayUrlTwo;
//    this.afterimageRelayUrl0000 = new Relay(afterimageRelayUrlTwo.replace("localhost", "0.0.0.0"));
    this.afterimageRelayUrl0000 = new Relay(afterimageRelayUrlTwo.replace("localhost", "afterimage-app"));
    this.afterimageRelayClient = new ReactiveNostrRelayClient(
//        afterimageRelayUrlTwo.replace("localhost", "0.0.0.0")
//        afterimageRelayUrl0000.getUrl()
        afterimageRelayUrlTwo
    );

    Identity voteSubmitterIdentity = Identity.create("aaa4585483196998204846989544737603523651520600328805626488477202");
    voteRecipientIdentity = // Identity.generateRandomIdentity();
        Identity.create("ccc4585483196998204846989544737603523651520600328805626488477202");

    awardUpvoteDefinitionEvent = new BadgeDefinitionGenericEvent(
        definitionsCreatorIdentity,
        upvoteIdentifierTag,
        superconductorRelayReplaced0000,
        String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", definitionsCreatorIdentity.getPublicKey()));

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent =
        new BadgeAwardGenericEvent<>(
            voteSubmitterIdentity,
            voteRecipientIdentity.getPublicKey(),
            superconductorRelayReplaced0000,
            awardUpvoteDefinitionEvent,
            String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", voteRecipientIdentity.getPublicKey()));

    FormulaEvent plusOneFormulaEvent = new FormulaEvent(
        definitionsCreatorIdentity,
        formulaIdentifierTag,
//        new Relay(afterimageRelayUrl),
        afterimageRelayUrl0000,
        awardUpvoteDefinitionEvent,
        PLUS_ONE_FORMULA);

    badgeDefinitionReputationEventPlusOneFormula = new BadgeDefinitionReputationEvent(
        definitionsCreatorIdentity,
        reputationIdentifierTag,
//        new Relay(afterimageRelayUrl),
        afterimageRelayUrl0000,
        AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
        plusOneFormulaEvent);

    log.debug("1of5 - sendEventToSuperconductor(awardUpvoteDefinitionEvent)");
    sendEventToSuperconductorRelay(awardUpvoteDefinitionEvent);
    TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("2of5 - sendEventToSuperconductor(badgeAwardUpvoteEvent)");
    sendEventToSuperconductorRelay(badgeAwardUpvoteEvent);
    TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("3of5 - sendEventToSuperconductor(plusOneFormulaEvent)");
    sendEventToAimgRelay(plusOneFormulaEvent);
    TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("4of5 - sendEventToSuperconductor(badgeDefinitionReputationEventPlusOneFormula)");
    sendEventToAimgRelay(badgeDefinitionReputationEventPlusOneFormula);
    TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("5of5 - sendEventToSuperconductor(badgeDefinitionReputationEventPlusOneFormula)");
    sendEventToAimgRelay(createSearchRelaysListEventMessage());
    log.debug("done 5of5, sleep 1000");
    TimeUnit.MILLISECONDS.sleep(1000);
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException, InterruptedException {
//    new AfterimageMeshRelayService(afterimageRelayUri)
//        .send(
//            new EventMessage(
//                createSearchRelaysListEventMessage(superconductorRelayUri_2)),
//            new TestSubscriber<>());
//

//    query Aimg for above REPUTATION event
    List<BaseMessage> afterImageEventsSubscriber_A = new SingleReqSubscriptionManager(afterimageRelayUrl, requestTimeoutDuration)
        .send(
            createAfterImageReqMessage(
                Factory.generateRandomHex64String(),
                voteRecipientIdentity.getPublicKey(),
                definitionsCreatorIdentity.getPublicKey()));

    log.debug("afterimage returned superconductor events:");
    List<EventIF> returnedReqGenericEvents_2 = getGenericEvents(afterImageEventsSubscriber_A);

    assertEquals("1", returnedReqGenericEvents_2.getFirst().getContent());

    ////    more SC events
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent_2 =
        new BadgeAwardGenericEvent<>(
            Identity.generateRandomIdentity(),
            voteRecipientIdentity.getPublicKey(),
            superconductorRelayReplaced0000,
            awardUpvoteDefinitionEvent,
            String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", voteRecipientIdentity.getPublicKey()));
//
//    assertEquals(event_2.getPublicKey().toHexString(), authorIdentity.getPublicKey().toHexString());
//
////  submit upvote event to SC
    sendEventToSuperconductorRelay(badgeAwardUpvoteEvent_2);
    TimeUnit.MILLISECONDS.sleep(1000);

    List<BaseMessage> afterImageEventsSubscriber_B = new SingleReqSubscriptionManager(afterimageRelayUrl, requestTimeoutDuration)
        .send(
            createAfterImageReqMessage(
                Factory.generateRandomHex64String(),
                voteRecipientIdentity.getPublicKey(),
                definitionsCreatorIdentity.getPublicKey()));

    log.debug("afterimage returned superconductor events:");
    List<EventIF> returnedReqGenericEvents_3 = getGenericEvents(afterImageEventsSubscriber_B);

    assertTrue(returnedReqGenericEvents_3.stream().map(EventIF::getContent).anyMatch("2"::equals));
    assertEquals(1, (long) returnedReqGenericEvents_3.size());
    assertEquals("2", returnedReqGenericEvents_3.getFirst().getContent());

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

  private ReqMessage createAfterImageReqMessage(String subscriberId, PublicKey upvotedUserPublicKey, PublicKey badgeCreatorPublicKey) throws JsonProcessingException {
    System.out.println("333333333333333333333");
    System.out.println("333333333333333333333");
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
                    reputationIdentifierTag,
                    afterimageRelayUrl0000)),
            new ReferencedPublicKeyFilter(
                new PubKeyTag(
                    upvotedUserPublicKey))));

    ReqMessage reqMessage = reqMessageWithStuff;
    log.debug(Util.prettyFormatJson(reqMessage.encode()));
    System.out.println("333333333333333333333");
    System.out.println("333333333333333333333");
    return reqMessage;
  }

  private BaseEvent createSearchRelaysListEventMessage() {
    Identity searchRelaysListEventSubmitterIdentity = Identity.generateRandomIdentity();
    log.debug("\n\n\nSearch Relays List sent from aImg IT 5556...");
    SearchRelaysListEvent searchRelaysListEvent = new SearchRelaysListEvent(
        searchRelaysListEventSubmitterIdentity,
        new RelaysTag(superconductorRelayReplaced0000),
        "Search Relays List sent from aImg IT 5556");
    return searchRelaysListEvent;
  }

  private void sendEventToSuperconductorRelay(BaseEvent baseEvent) {
    final String RED_BOLD_BRIGHT = "\033[1;91m";
    final String GREEN_BOLD = "\033[1;32m";
    final String RESET = "\033[0m";
    String greenFont = GREEN_BOLD + "%s" + RESET;
    String redFont = RED_BOLD_BRIGHT + "%s" + RESET;

    final RequestSubscriber<OkMessage> subscriber = new RequestSubscriber<>(DurationFactory.of(5, TimeUnit.SECONDS));
//    final TestSubscriber<OkMessage> subscriber = new TestSubscriber<>();
    superconductorRelayClient.send(
        new EventMessage(baseEvent),
        subscriber);
    List<OkMessage> scReturnedOkMessage = subscriber.getItems();
    Boolean flag = scReturnedOkMessage.getFirst().getFlag();
    log.debug("\n  ***********  OKMessage received from SC relay? [{}] ************\n",
        String.format(flag ? greenFont : redFont, flag.toString().toUpperCase()));
//    assertEquals(true, upvoteDefinitionOkMessageItems.getFirst().getFlag());
//    subscriber.dispose();
//    new AfterimageMeshRelayService(afterimageRelayUrl2).closeSocket();
  }

  private void sendEventToAimgRelay(BaseEvent baseEvent) throws InterruptedException {
    final String RED_BOLD_BRIGHT = "\033[1;91m";
    final String GREEN_BOLD = "\033[1;32m";
    final String RESET = "\033[0m";
    String greenFont = GREEN_BOLD + "%s" + RESET;
    String redFont = RED_BOLD_BRIGHT + "%s" + RESET;

    final RequestSubscriber<OkMessage> subscriber = new RequestSubscriber<>(DurationFactory.of(5, TimeUnit.SECONDS));
//    final TestSubscriber<OkMessage> subscriber = new TestSubscriber<>();
    afterimageRelayClient.send(
        new EventMessage(baseEvent),
        subscriber);
    TimeUnit.MILLISECONDS.sleep(1000);
    List<OkMessage> scReturnedOkMessage = subscriber.getItems();
    Boolean flag = scReturnedOkMessage.getFirst().getFlag();
    log.debug("\n  ***********  OKMessage received from Aimg relay? [{}] ************\n",
        String.format(flag ? greenFont : redFont, flag.toString().toUpperCase()));
//    assertEquals(true, upvoteDefinitionOkMessageItems.getFirst().getFlag());
//    subscriber.dispose();
//    new AfterimageMeshRelayService(afterimageRelayUrl2).closeSocket();
  }
}
