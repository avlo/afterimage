//package com.prosilion.afterimage.service.reactive;
//
//import com.ezylang.evalex.parser.ParseException;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.prosilion.afterimage.config.MultiContainerTestConfig;
//import com.prosilion.afterimage.enums.AfterimageKindType;
//import com.prosilion.afterimage.util.Factory;
//import com.prosilion.nostr.NostrException;
//import com.prosilion.nostr.enums.Kind;
//import com.prosilion.nostr.event.BadgeAwardGenericEvent;
//import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
//import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
//import com.prosilion.nostr.event.BaseEvent;
//import com.prosilion.nostr.event.EventIF;
//import com.prosilion.nostr.event.FormulaEvent;
//import com.prosilion.nostr.event.RelaySetsEvent;
//import com.prosilion.nostr.event.SearchRelaysListEvent;
//import com.prosilion.nostr.event.internal.Relay;
//import com.prosilion.nostr.filter.Filters;
//import com.prosilion.nostr.filter.event.KindFilter;
//import com.prosilion.nostr.filter.tag.AddressTagFilter;
//import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
//import com.prosilion.nostr.message.BaseMessage;
//import com.prosilion.nostr.message.EventMessage;
//import com.prosilion.nostr.message.OkMessage;
//import com.prosilion.nostr.message.ReqMessage;
//import com.prosilion.nostr.tag.AddressTag;
//import com.prosilion.nostr.tag.IdentifierTag;
//import com.prosilion.nostr.tag.PubKeyTag;
//import com.prosilion.nostr.tag.RelaysTag;
//import com.prosilion.nostr.user.Identity;
//import com.prosilion.nostr.user.PublicKey;
//import com.prosilion.nostr.util.Util;
//import com.prosilion.subdivisions.client.reactive.ReactiveNostrRelayClient;
//import com.prosilion.superconductor.base.util.RequestSubscriber;
//import com.prosilion.superconductor.base.util.SingleReqSubscriptionManager;
//import java.io.IOException;
//import java.time.Duration;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.lang.NonNull;
//import org.springframework.test.context.ActiveProfiles;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@Slf4j
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
//@ActiveProfiles("test")
//@Import(MultiContainerTestConfig.class)
//public class RelaySetsIT {
//  /**
//   * definitionsCreatorIdentity:   02d49b23e02985a760e8bc2f5ee86a3089569806f5f6a670fba3317568d14262
//   * <p>
//   * voteSubmitterIdentity:        611eda70943b4f67d1674068f5c86cedbdc3438bb41245b129a6311e4f308295
//   * <p>
//   * voteReceierIdentity:          985a5b9ea911bb8f9d9dca82c03f776d68fdc452b774295a874423a0fa5e8879
//   */
//
//  public static final String REPUTATION = "TEST_REPUTATION";
//  public static final String AWARD_UNIT_UPVOTE = "TEST_UNIT_UPVOTE";
//  public static final String FORMULA_UNIT_UPVOTE = "FORMULA_UNIT_UPVOTE";
//
//  public static final String PLUS_ONE_FORMULA = "+1";
//
//  private final IdentifierTag reputationIdentifierTag = new IdentifierTag(REPUTATION);
//  private final IdentifierTag upvoteIdentifierTag = new IdentifierTag(AWARD_UNIT_UPVOTE);
//  private final IdentifierTag formulaIdentifierTag = new IdentifierTag(FORMULA_UNIT_UPVOTE);
//
//  private final Identity definitionsCreatorIdentity = // Identity.generateRandomIdentity();
//      Identity.create("bbb4585483196998204846989544737603523651520600328805626488477202");
//
//  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventPlusOneFormula;
//
//  private final Identity voteRecipientIdentity;
//  private final Relay superconductorRelayReplaced_SC_REPLACEMENT;
//  private final Relay afterimageRelayReplaced0000;
//  private final Relay afterimageRelayTwoReplaced_AIMG_REPLACEMENT;
//  private final String afterimageRelayUrl;
//  private final Duration requestTimeoutDuration;
//  private final ReactiveNostrRelayClient superconductorRelayClient;
//  private final ReactiveNostrRelayClient afterimageRelayClient;
//  private final ReactiveNostrRelayClient afterimageRelayClientTwo;
//
//  BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent;
//  @Autowired private String superconductorRelayUrl;
//
//  @Autowired
//  public RelaySetsIT(
//      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl,
//      @NonNull @Value("${afterimage.relay.url.two}") String afterimageRelayUrlTwo,
//      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
//      @NonNull Duration requestTimeoutDuration) throws ParseException, InterruptedException, IOException {
//
//    this.afterimageRelayUrl = afterimageRelayUrl;
//    this.requestTimeoutDuration = requestTimeoutDuration;
//
//    this.superconductorRelayClient = new ReactiveNostrRelayClient(superconductorRelayUrl.replace("localhost", "0.0.0.0"));
//    this.afterimageRelayClient = new ReactiveNostrRelayClient(afterimageRelayUrl.replace("localhost", "0.0.0.0"));
//
//    voteRecipientIdentity = // Identity.generateRandomIdentity();
//        Identity.create("ccc4585483196998204846989544737603523651520600328805626488477202");
//
//    this.superconductorRelayReplaced_SC_REPLACEMENT = new Relay(superconductorRelayUrl.replace("localhost", "superconductor-afterimage"));
//    awardUpvoteDefinitionEvent = new BadgeDefinitionGenericEvent(
//        definitionsCreatorIdentity,
//        upvoteIdentifierTag,
//        superconductorRelayReplaced_SC_REPLACEMENT,
//        String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", definitionsCreatorIdentity.getPublicKey()));
//
//    this.afterimageRelayReplaced0000 = new Relay(afterimageRelayUrl.replace("localhost", "0.0.0.0"));
//    this.afterimageRelayTwoReplaced_AIMG_REPLACEMENT = new Relay(afterimageRelayUrlTwo.replace("localhost", "afterimage-app"));
//    this.afterimageRelayClientTwo = new ReactiveNostrRelayClient(afterimageRelayUrlTwo.replace("localhost", "0.0.0.0"));
//
//    FormulaEvent plusOneFormulaEvent = new FormulaEvent(
//        definitionsCreatorIdentity,
//        formulaIdentifierTag,
//        afterimageRelayTwoReplaced_AIMG_REPLACEMENT,
////        afterimageRelayReplaced0000,
//        awardUpvoteDefinitionEvent,
//        PLUS_ONE_FORMULA);
//
//    badgeDefinitionReputationEventPlusOneFormula = new BadgeDefinitionReputationEvent(
//        definitionsCreatorIdentity,
//        reputationIdentifierTag,
//        afterimageRelayTwoReplaced_AIMG_REPLACEMENT,
////        new Relay(afterimageRelayUrl),
//        AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
//        plusOneFormulaEvent);
//
//    log.debug("1of3 - sendEventToSuperconductor(awardUpvoteDefinitionEvent)");
//    sendEventToSuperconductorRelay(awardUpvoteDefinitionEvent, superconductorRelayClient);
//    TimeUnit.MILLISECONDS.sleep(1000);
//
//    log.debug("2of3 - sendEventToSuperconductor(plusOneFormulaEvent)");
//    sendEventToAimgRelay(plusOneFormulaEvent, afterimageRelayClientTwo);
//    TimeUnit.MILLISECONDS.sleep(1000);
//
//    log.debug("3of3 - sendEventToSuperconductor(badgeDefinitionReputationEventPlusOneFormula)");
//    sendEventToAimgRelay(badgeDefinitionReputationEventPlusOneFormula, afterimageRelayClientTwo);
//    TimeUnit.MILLISECONDS.sleep(1000);
//  }
//
//  @Test
//  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException, InterruptedException {
//    Identity voteSubmitterIdentity = Identity.create("aaa4585483196998204846989544737603523651520600328805626488477202");
//
//    List<BaseMessage> afterImageEventsSubscriber_A = new SingleReqSubscriptionManager(afterimageRelayUrl, requestTimeoutDuration)
//        .send(
//            createAfterImageReqMessage(
//                Factory.generateRandomHex64String(),
//                voteRecipientIdentity.getPublicKey(),
//                definitionsCreatorIdentity.getPublicKey()));
//
//    //  test initial aImg events state, should have zero reputation events for upvotedUser
//    log.debug("afterimage initial events:");
//    List<EventIF> initialEvents = getGenericEvents(afterImageEventsSubscriber_A);
//    assertEquals(0, initialEvents.size());
//
////  submit SC vote(s)  
//    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> event =
//        new BadgeAwardGenericEvent<>(
//            voteSubmitterIdentity,
//            voteRecipientIdentity.getPublicKey(),
//            superconductorRelayReplaced_SC_REPLACEMENT,
//            awardUpvoteDefinitionEvent,
//            String.format("badgeAwardUpvoteEvent 1, vote recipient PublicKey: [%s]", voteRecipientIdentity.getPublicKey()));
//
//    //  submit 1of2 upvote event to SC
//    sendEventToSuperconductorRelay(event, superconductorRelayClient);
//    TimeUnit.MILLISECONDS.sleep(500);
//
//    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> event_new =
//        new BadgeAwardGenericEvent<>(
//            voteSubmitterIdentity,
//            voteRecipientIdentity.getPublicKey(),
//            superconductorRelayReplaced_SC_REPLACEMENT,
//            awardUpvoteDefinitionEvent,
//            String.format("badgeAwardUpvoteEvent 1, vote recipient PublicKey: [%s]", voteRecipientIdentity.getPublicKey()));
//
//    //  submit 2of2 upvote event to SC
//    sendEventToSuperconductorRelay(event_new, superconductorRelayClient);
//    TimeUnit.MILLISECONDS.sleep(500);
//
//    sendEventToAimgRelay(createSearchRelaysListEventMessage(), afterimageRelayClientTwo);
//
////    TODO: check aImgDocker reputation, should have "2"
////    List<BaseMessage> aImgDockerEventsSubscriber = new SingleReqSubscriptionManager(afterimageRelayUrlTwo, requestTimeoutDuration)
////        .send(
////            createAfterImageReqMessage(
////                Factory.generateRandomHex64String(),
////                voteRecipientIdentity.getPublicKey(),
////                definitionsCreatorIdentity.getPublicKey()));
////
////    TimeUnit.MILLISECONDS.sleep(1000);
////
////    log.debug("afterimage returned superconductor events:");
////    List<EventIF> returnedEventsAImg = getGenericEvents(aImgDockerEventsSubscriber);
////    assertEquals(1, returnedEventsAImg.size());
////    assertEquals("2", returnedEventsAImg.getFirst().getContent());
//
////    submit RelaySets event to aImg containing aImg docker as a RelaySets source
//    sendEventToAimgRelay(createRelaysSetsEventMessage(afterimageRelayTwoReplaced_AIMG_REPLACEMENT), afterimageRelayClient);
////    sendEventToAimgRelay(createRelaysSetsEventMessage(afterimageRelayTwoReplaced0000.getUrl()), afterimageRelayClient);
//
//    TimeUnit.MILLISECONDS.sleep(1000);
//
////  query Aimg for REPUTATION event existence
//    List<BaseMessage> afterimageRepRequestClient = new SingleReqSubscriptionManager(afterimageRelayUrl, requestTimeoutDuration)
//        .send(
//            createAfterImageReqMessage(
//                Factory.generateRandomHex64String(),
//                voteRecipientIdentity.getPublicKey(),
//                definitionsCreatorIdentity.getPublicKey()));
//
//    TimeUnit.MILLISECONDS.sleep(100);
//
//    log.debug("afterimage returned superconductor events:");
//    List<EventIF> returnedEvents = getGenericEvents(afterimageRepRequestClient);
//    assertEquals(1, returnedEvents.size());
//    assertEquals("2", returnedEvents.getFirst().getContent());
//  }
//
//  private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
//    return returnedBaseMessages.stream()
//        .filter(EventMessage.class::isInstance)
//        .map(EventMessage.class::cast)
//        .map(EventMessage::getEvent)
//        .toList();
//  }
//
//  private ReqMessage createAfterImageReqMessage(String subscriberId, PublicKey upvotedUserPublicKey, PublicKey badgeCreatorPublicKey) throws JsonProcessingException {
//    System.out.println("333333333333333333333");
//    System.out.println("333333333333333333333");
//    ReqMessage reqMessageWithStuff = new ReqMessage(
//        subscriberId,
//        new Filters(
//            new KindFilter(
//                Kind.BADGE_AWARD_EVENT),
////            new IdentifierTagFilter(reputationIdentifierTag),
//            new AddressTagFilter(
//                new AddressTag(
//                    Kind.BADGE_DEFINITION_EVENT,
//                    badgeCreatorPublicKey,
//                    reputationIdentifierTag,
//                    new Relay(afterimageRelayUrl))),
//            new ReferencedPublicKeyFilter(
//                new PubKeyTag(
//                    upvotedUserPublicKey))));
//
//    ReqMessage reqMessage = reqMessageWithStuff;
//    log.debug(Util.prettyFormatJson(reqMessage.encode()));
//    System.out.println("333333333333333333333");
//    System.out.println("333333333333333333333");
//    return reqMessage;
//  }
//
//  private RelaySetsEvent createRelaysSetsEventMessage(Relay relay) {
//    Identity searchRelaysListEventSubmitterIdentity = Identity.generateRandomIdentity();
//    return new RelaySetsEvent(
//        searchRelaysListEventSubmitterIdentity,
//        new RelaysTag(relay),
//        "Kind.RELAY_SETS");
//  }
//
//  private SearchRelaysListEvent createSearchRelaysListEventMessage() {
//    Identity searchRelaysListEventSubmitterIdentity = Identity.generateRandomIdentity();
//    log.debug("Search Relays List sent from aImg IT 5556...");
//    SearchRelaysListEvent searchRelaysListEvent = new SearchRelaysListEvent(
//        searchRelaysListEventSubmitterIdentity,
//        new RelaysTag(new Relay(superconductorRelayUrl)),
//        "Search Relays List sent from aImg IT 5556");
//    return searchRelaysListEvent;
//  }
//
//  private void sendEventToAimgRelay(BaseEvent baseEvent, ReactiveNostrRelayClient relayClient) throws InterruptedException, IOException {
//    final String RED_BOLD_BRIGHT = "\033[1;91m";
//    final String GREEN_BOLD = "\033[1;32m";
//    final String RESET = "\033[0m";
//    String greenFont = GREEN_BOLD + "%s" + RESET;
//    String redFont = RED_BOLD_BRIGHT + "%s" + RESET;
//
//    final RequestSubscriber<OkMessage> subscriber = new RequestSubscriber<>(requestTimeoutDuration);
//    relayClient.send(
//        new EventMessage(baseEvent),
//        subscriber);
//    TimeUnit.MILLISECONDS.sleep(1800);
//    List<OkMessage> scReturnedOkMessage = subscriber.getItems();
//    Boolean flag = scReturnedOkMessage.getFirst().getFlag();
//    log.debug("***********  OKMessage received from Aimg relay? [{}] ************",
//        String.format(flag ? greenFont : redFont, flag.toString().toUpperCase()));
//  }
//
//  private void sendEventToSuperconductorRelay(BaseEvent baseEvent, ReactiveNostrRelayClient superconductorRelayClient) throws InterruptedException, IOException {
//    final String RED_BOLD_BRIGHT = "\033[1;91m";
//    final String GREEN_BOLD = "\033[1;32m";
//    final String RESET = "\033[0m";
//    String greenFont = GREEN_BOLD + "%s" + RESET;
//    String redFont = RED_BOLD_BRIGHT + "%s" + RESET;
//
//    final RequestSubscriber<OkMessage> subscriber = new RequestSubscriber<>(requestTimeoutDuration);
//    superconductorRelayClient.send(
//        new EventMessage(baseEvent),
//        subscriber);
//    TimeUnit.MILLISECONDS.sleep(1000);
//    List<OkMessage> scReturnedOkMessage = subscriber.getItems();
//    Boolean flag = scReturnedOkMessage.getFirst().getFlag();
//    log.debug("***********  OKMessage received from SC relay? [{}] ************",
//        String.format(flag ? greenFont : redFont, flag.toString().toUpperCase()));
//  }
//}
