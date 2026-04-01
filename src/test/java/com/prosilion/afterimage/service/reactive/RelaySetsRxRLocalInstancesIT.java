package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.RelaySetsEvent;
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
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.subdivisions.client.reactive.NostrEventPublisher;
import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
import com.prosilion.subdivisions.client.reactive.SingleRelaySubscriptionsManager;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class RelaySetsRxRLocalInstancesIT {
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

  private final Identity definitionsCreatorIdentity = // Identity.generateRandomIdentity();
      Identity.create("bbb4585483196998204846989544737603523651520600328805626488477202");

  private final Identity voteRecipientIdentity;

  private final String superconductorRelayUrl;
  private final String afterimageRelayUrlTwo;
  private final String afterimageRelayUrlThree;

  private final Relay superconductorDockerRelay;
  private final Relay afterimageDockerRelayUrlTwo;
  private final Relay afterimageDockerRelayUrlThree;

  BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent;

  public RelaySetsRxRLocalInstancesIT() throws ParseException, JsonProcessingException {

    this.superconductorRelayUrl = "ws://localhost:5555";
    this.afterimageRelayUrlTwo = "ws://localhost:5557";
    this.afterimageRelayUrlThree = "ws://localhost:5558";

    this.superconductorDockerRelay = new Relay(superconductorRelayUrl);
    this.afterimageDockerRelayUrlTwo = new Relay(afterimageRelayUrlTwo);
    this.afterimageDockerRelayUrlThree = new Relay(afterimageRelayUrlThree);

    Identity voteSubmitterIdentity = Identity.create("aaa4585483196998204846989544737603523651520600328805626488477202");
    voteRecipientIdentity = // Identity.generateRandomIdentity();
        Identity.create("ccc4585483196998204846989544737603523651520600328805626488477202");

    awardUpvoteDefinitionEvent = new BadgeDefinitionGenericEvent(
        definitionsCreatorIdentity,
        new IdentifierTag(AWARD_UNIT_UPVOTE),
        superconductorDockerRelay,
        String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", definitionsCreatorIdentity.getPublicKey()));

    log.debug("1of6 - sendEventToSuperconductor(awardUpvoteDefinitionEvent)");
    sendEventToRelay(awardUpvoteDefinitionEvent, superconductorRelayUrl);

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent =
        new BadgeAwardGenericEvent<>(
            voteSubmitterIdentity,
            voteRecipientIdentity.getPublicKey(),
            superconductorDockerRelay,
            awardUpvoteDefinitionEvent,
            String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", voteRecipientIdentity.getPublicKey()));

    log.debug("2of6 - sendEventToRelay(badgeAwardUpvoteEvent, superconductorRelayUrl)");
    sendEventToRelay(badgeAwardUpvoteEvent, superconductorRelayUrl);

    FormulaEvent plusOneFormulaEvent = new FormulaEvent(
        definitionsCreatorIdentity,
        new IdentifierTag(FORMULA_UNIT_UPVOTE),
        afterimageDockerRelayUrlTwo,
        awardUpvoteDefinitionEvent,
        PLUS_ONE_FORMULA);

    log.debug("3of6 - sendEventToRelay(plusOneFormulaEvent, afterimageRelayUrlTwo)");
    sendEventToRelay(plusOneFormulaEvent, afterimageRelayUrlTwo);

    log.debug("4of6 - sendEventToRelay(badgeDefinitionReputationEventPlusOneFormula, afterimageRelayUrlTwo)");
    sendEventToRelay(new BadgeDefinitionReputationEvent(
        definitionsCreatorIdentity,
        reputationIdentifierTag,
        afterimageDockerRelayUrlTwo,
        AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
        plusOneFormulaEvent), afterimageRelayUrlTwo);

    log.debug("5of6 - sendEventToRelay(createSearchRelaysListEventMessage(), afterimageRelayUrlTwo)");
    sendEventToRelay(createSearchRelaysListEventMessage(), afterimageRelayUrlTwo);

    log.debug("6of6 - sendEventToRelay(createRelaysSetsEventMessage(), afterimageRelayUrlThree)");
    sendEventToRelay(createRelaysSetsEventMessage(), afterimageRelayUrlThree);

//    query Aimg for above REPUTATION event
    RequestSubscriber<BaseMessage> reputationEventSubscriber = new RequestSubscriber<>();
    SingleRelaySubscriptionsManager manager = new NostrSingleRequestService()
        .send(
            createAfterImageReqMessage(
                Factory.generateRandomHex64String(),
                voteRecipientIdentity.getPublicKey(),
                definitionsCreatorIdentity.getPublicKey()),
            afterimageRelayUrlThree,
            reputationEventSubscriber);
    List<BaseMessage> afterImageEventsSubscriber_A = reputationEventSubscriber.getItems();
    log.debug("afterimage returned superconductor events:");
    List<EventIF> returnedReqGenericEvents_2 = getGenericEvents(afterImageEventsSubscriber_A);
    reputationEventSubscriber.dispose();
    manager.closeAllSessions();

    assertEquals("1", returnedReqGenericEvents_2.getFirst().getContent());

    ////    more SC events
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent_2 =
        new BadgeAwardGenericEvent<>(
            Identity.generateRandomIdentity(),
            voteRecipientIdentity.getPublicKey(),
            superconductorDockerRelay,
            awardUpvoteDefinitionEvent,
            String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", voteRecipientIdentity.getPublicKey()));
//
//    assertEquals(event_2.getPublicKey().toHexString(), authorIdentity.getPublicKey().toHexString());
//
////  submit upvote event to SC
    sendEventToRelay(badgeAwardUpvoteEvent_2, superconductorRelayUrl);

    RequestSubscriber<BaseMessage> reputationEventSubscriber_B = new RequestSubscriber<>();
    SingleRelaySubscriptionsManager manager1 = new NostrSingleRequestService()
        .send(
            createAfterImageReqMessage(
                Factory.generateRandomHex64String(),
                voteRecipientIdentity.getPublicKey(),
                definitionsCreatorIdentity.getPublicKey()),
            afterimageRelayUrlThree,
            reputationEventSubscriber_B);
    reputationEventSubscriber_B.dispose();
    manager1.closeAllSessions();

    List<BaseMessage> afterImageEventsSubscriber_B = reputationEventSubscriber_B.getItems();
    log.debug("afterimage returned superconductor events:");
    List<EventIF> returnedReqGenericEvents_3 = getGenericEvents(afterImageEventsSubscriber_B);

    assertTrue(returnedReqGenericEvents_3.stream().map(EventIF::getContent).anyMatch("2"::equals));
    assertEquals(1, (long) returnedReqGenericEvents_3.size());
    assertEquals("2", returnedReqGenericEvents_3.getFirst().getContent());
  }

  @Test
  void doSomeTesting() {
    log.debug("all test/results are in constructor.");
  }

  private BaseEvent createSearchRelaysListEventMessage() {
    Identity searchRelaysListEventSubmitterIdentity = Identity.generateRandomIdentity();
    log.debug("Search Relays List sent from aImg IT 5556...");
    return new SearchRelaysListEvent(
        searchRelaysListEventSubmitterIdentity,
        new RelaysTag(superconductorDockerRelay),
        "Search Relays List sent from aImg IT 5556");
  }

  private RelaySetsEvent createRelaysSetsEventMessage() {
    Identity searchRelaysListEventSubmitterIdentity = Identity.generateRandomIdentity();
    return new RelaySetsEvent(
        searchRelaysListEventSubmitterIdentity,
        new RelaysTag(afterimageDockerRelayUrlTwo),
        "Kind.RELAY_SETS");
  }

  private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
  }

  private ReqMessage createAfterImageReqMessage(String subscriberId, PublicKey upvotedUserPublicKey, PublicKey badgeCreatorPublicKey) throws JsonProcessingException {
    ReqMessage reqMessageWithStuff = new ReqMessage(
        subscriberId,
        new Filters(
            new KindFilter(
                Kind.BADGE_AWARD_EVENT),
            new AddressTagFilter(
                new AddressTag(
                    Kind.BADGE_DEFINITION_EVENT,
                    badgeCreatorPublicKey,
                    reputationIdentifierTag,
                    afterimageDockerRelayUrlThree)),
            new ReferencedPublicKeyFilter(
                new PubKeyTag(
                    upvotedUserPublicKey))));

    ReqMessage reqMessage = reqMessageWithStuff;
    log.debug(Util.prettyFormatJson(reqMessage.encode()));
    return reqMessage;
  }

  private void sendEventToRelay(BaseEvent baseEvent, String relayUrl) {
    RequestSubscriber<OkMessage> eventSubscriber = new RequestSubscriber<>();
    new NostrEventPublisher(relayUrl).send(
        new EventMessage(baseEvent), eventSubscriber);
    OkMessage scReturnedOkMessage = eventSubscriber.getItems().getFirst();
    Boolean flag = scReturnedOkMessage.getFlag();
    log.debug("***********  OKMessage received from [{}] relay? [{}] ************",
        relayUrl, flag.toString().toUpperCase());
  }
}
