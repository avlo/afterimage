package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.config.TestcontainersConfig;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.RelaySetsEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.filter.tag.ExternalIdentityTagFilter;
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
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import java.io.IOException;
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

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public class FollowSetsIT {
  private final static IdentifierTag relaySetsIdentifierTag = new IdentifierTag("TODO:RELAY_SETS_IDENTIFIER_TAG");

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
  private final Relay afterimageRelay;

  private final Identity afterimageIdentity;
  private final Identity voteRecipientIdentity;

  private final String afterimageRelayUrl2;
  private final Relay aimgSelfRefencingRelay;

  @Autowired
  public FollowSetsIT(
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull String afterimageRelayUrl,
      @NonNull @Value("${afterimage.relay.url.two}") String afterimageRelayUrl2,
      @NonNull CacheServiceIF cacheService) throws ParseException, IOException, InterruptedException {
    this.afterimageRelayUrl2 = afterimageRelayUrl2;
    this.afterimageIdentity = afterimageInstanceIdentity;
    this.aimgSelfRefencingRelay = new Relay("ws://localhost:5556");

    Identity voteSubmitterIdentity = Identity.create("aaa4585483196998204846989544737603523651520600328805626488477202");
    voteRecipientIdentity = // Identity.generateRandomIdentity();
        Identity.create("ccc4585483196998204846989544737603523651520600328805626488477202");

    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
    log.debug("voteReceiverPubkey:  {}  ", voteRecipientIdentity.getPublicKey().toHexString());
    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
    afterimageRelay = new Relay(afterimageRelayUrl);

    BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent = new BadgeDefinitionGenericEvent(
        definitionsCreatorIdentity,
        upvoteIdentifierTag,
        aimgSelfRefencingRelay);

    FormulaEvent plusOneFormulaEvent = new FormulaEvent(
        definitionsCreatorIdentity,
        upvoteIdentifierTag,
        aimgSelfRefencingRelay,
        awardUpvoteDefinitionEvent,
        PLUS_ONE_FORMULA);

    badgeDefinitionReputationEventPlusOneFormula = new BadgeDefinitionReputationEvent(
        definitionsCreatorIdentity,
        reputationIdentifierTag,
        aimgSelfRefencingRelay,
        AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
        plusOneFormulaEvent);

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent =
        new BadgeAwardGenericEvent<>(
            voteSubmitterIdentity,
            voteRecipientIdentity.getPublicKey(),
            aimgSelfRefencingRelay,
            badgeDefinitionReputationEventPlusOneFormula);

////    reminder : do lsports for relay2's docker internal url ...
//    sendEventToAimgRelay2(awardUpvoteDefinitionEvent);
    cacheService.save(awardUpvoteDefinitionEvent);
    TimeUnit.MILLISECONDS.sleep(100);

////    ... req'd by events' validation / relay  processing calls    
//    sendEventToAimgRelay2(plusOneFormulaEvent);
    cacheService.save(plusOneFormulaEvent);
    TimeUnit.MILLISECONDS.sleep(100);

////    ... req'd by events' validation / relay  processing calls    
//    sendEventToAimgRelay2(badgeDefinitionReputationEventPlusOneFormula);
    cacheService.save(badgeDefinitionReputationEventPlusOneFormula);
    TimeUnit.MILLISECONDS.sleep(100);

////    ... req'd by events' validation / relay  processing calls    
//    cacheService.save(badgeAwardUpvoteEvent);
    sendEventToAimgRelay2(badgeAwardUpvoteEvent);
  }

  @Test
  void testFollowSetsEvent() throws IOException, InterruptedException {
//    TODO: below may already get created by aimg2
//    FollowSetsEvent followSetsEvent = new FollowSetsEvent(
//        afterimageIdentity,
//        voteRecipientIdentity.getPublicKey(),
//        badgeDefinitionReputationEventPlusOneFormula.getIdentifierTag(),
//        aimgSelfRefencingRelay,
//        List.of(badgeAwardUpvoteEvent));
//
////    String aimg5557 = "ws://localhost:5557";
//    new AfterimageMeshRelayService(afterimageRelayUrl2)
//        .send(
//            new EventMessage(
//                followSetsEvent),
//            new TestSubscriber<>());
//    TimeUnit.MILLISECONDS.sleep(5000);
//    TODO END: below may already get created by aimg2    

//      then send awareness to 5556 of RELAY SETS 5557
    new AfterimageMeshRelayService(afterimageRelay.getUrl())
        .send(
            new EventMessage(
//      ...announcing 5557               
                createRelaysSetsEventMessage(afterimageRelayUrl2)),
            new TestSubscriber<>());

    TimeUnit.MILLISECONDS.sleep(5000);

//      sent REP request to 5556
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_A = new TestSubscriber<>();
    final AfterimageMeshRelayService afterimageRepRequestClient = new AfterimageMeshRelayService(afterimageRelay.getUrl());
    afterimageRepRequestClient.send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            voteRecipientIdentity.getPublicKey(),
            definitionsCreatorIdentity.getPublicKey()),
        afterImageEventsSubscriber_A);

    TimeUnit.MILLISECONDS.sleep(100);

    log.debug("afterimage returned superconductor events:");
    List<BaseMessage> items_3 = afterImageEventsSubscriber_A.getItems();
    log.debug("  {}", items_3);

    List<EventIF> returnedReqGenericEvents_2 = getGenericEvents(items_3);

    assertEquals("2", returnedReqGenericEvents_2.getFirst().getContent());
  }

  private ReqMessage createAfterImageReqMessage(
      String subscriberId,
      PublicKey upvotedUserPublicKey,
      PublicKey badgeCreatorPublicKey) throws JsonProcessingException {
    System.out.println("333333333333333333333");
    System.out.println("333333333333333333333");
    ExternalIdentityTagFilter externalIdentityTagFilter = new ExternalIdentityTagFilter(BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG);
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
                    upvotedUserPublicKey)),
            externalIdentityTagFilter));

    ReqMessage reqMessage = reqMessageWithStuff;
    log.debug(Util.prettyFormatJson(reqMessage.encode()));
    System.out.println("333333333333333333333");
    System.out.println("333333333333333333333");
    return reqMessage;
  }

  private BaseEvent createRelaysSetsEventMessage(String uri) {
    return new RelaySetsEvent(
        afterimageIdentity,
        relaySetsIdentifierTag,
        "Kind.RELAY_SETS",
        new RelaysTag(
            new Relay(uri)));
  }

  private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
  }

  private void sendEventToAimgRelay2(BaseEvent baseEvent) throws IOException, InterruptedException {
    final String RED_BOLD_BRIGHT = "\033[1;91m";
    final String GREEN_BOLD = "\033[1;32m";
    final String RESET = "\033[0m";
    String greenFont = GREEN_BOLD + "%s" + RESET;
    String redFont = RED_BOLD_BRIGHT + "%s" + RESET;

    final TestSubscriber<OkMessage> subscriber = new TestSubscriber<>();
    new AfterimageMeshRelayService(afterimageRelayUrl2).send(
        new EventMessage(baseEvent),
        subscriber);
    List<OkMessage> upvoteDefinitionOkMessageItems = subscriber.getItems();
    Boolean flag = upvoteDefinitionOkMessageItems.getFirst().getFlag();
    log.debug("\n\n  ***********  OKMessage received from sendEventToAimgRelay2? [{}] ************\n",
        String.format(flag ? greenFont : redFont, flag.toString().toUpperCase()));
//    assertEquals(true, upvoteDefinitionOkMessageItems.getFirst().getFlag());
    subscriber.dispose();
//    new AfterimageMeshRelayService(afterimageRelayUrl2).closeSocket();
    TimeUnit.MILLISECONDS.sleep(250);
  }

//  private EventTagAddressTagPair createPair(String eventId, String afterimageRelayUrl) {
//    return new EventTagAddressTagPair(
//        new EventTag(
//            eventId,
//            afterimageRelayUrl),
//        new AddressTag(
//            Kind.BADGE_AWARD_EVENT,
//            authorIdentity.getPublicKey(),
//            new IdentifierTag(
//                new IdentifierTag(AfterimageBaseConfig.UNIT_UPVOTE).getUuid())));
//  }
}
