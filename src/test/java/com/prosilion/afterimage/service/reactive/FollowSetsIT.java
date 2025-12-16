package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.TestcontainersConfig;
import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericVoteEvent;
import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.RelaySetsEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public class FollowSetsIT {
  public static final IdentifierTag relaySetsIdentifierTag = new IdentifierTag("TODO:RELAY_SETS_IDENTIFIER_TAG");

  private final Relay relay;
  public static final String REPUTATION = "TEST_REPUTATION";
  public static final String UNIT_UPVOTE = "TEST_UNIT_UPVOTE";
  public static final String PLUS_ONE_FORMULA = "+1";

  public final IdentifierTag reputationIdentifierTag = new IdentifierTag(REPUTATION);
  public final IdentifierTag upvoteIdentifierTag = new IdentifierTag(UNIT_UPVOTE);

  public final Identity identity = Identity.generateRandomIdentity();

  public static final String PLATFORM = FollowSetsIT.class.getPackageName();
  public static final String IDENTITY = FollowSetsIT.class.getSimpleName();
  public static final String PROOF = String.valueOf(FollowSetsIT.class.hashCode());

  private final BadgeDefinitionAwardEvent awardUpvoteDefinitionEvent;
  private final FormulaEvent plusOneFormulaEvent;
  ;
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventPlusOneFormula;

  private final PublicKey reputationRecipientPublicKey = Identity.generateRandomIdentity().getPublicKey();
  private final BadgeAwardGenericVoteEvent badgeAwardUpvoteEvent;

  Identity afterimageInstanceIdentity;

  @Autowired
  public FollowSetsIT(
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull String afterimageRelayUrl,
      CacheServiceIF cacheService) throws ParseException {
    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
    System.out.print("voteReceiverPubkey:\n  ");
    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
    relay = new Relay(afterimageRelayUrl);

    this.awardUpvoteDefinitionEvent = new BadgeDefinitionAwardEvent(identity, upvoteIdentifierTag, relay);
    this.plusOneFormulaEvent = new FormulaEvent(identity, upvoteIdentifierTag, relay, awardUpvoteDefinitionEvent, PLUS_ONE_FORMULA);

    badgeDefinitionReputationEventPlusOneFormula = new BadgeDefinitionReputationEvent(
        identity,
        reputationIdentifierTag,
        relay,
        new ExternalIdentityTag(PLATFORM, IDENTITY, PROOF),
        plusOneFormulaEvent);

    badgeAwardUpvoteEvent = new BadgeAwardGenericVoteEvent(
        identity,
        reputationRecipientPublicKey,
        badgeDefinitionReputationEventPlusOneFormula);

    this.afterimageInstanceIdentity = afterimageInstanceIdentity;

    cacheService.save(awardUpvoteDefinitionEvent);
    cacheService.save(plusOneFormulaEvent);
    cacheService.save(badgeDefinitionReputationEventPlusOneFormula);
    cacheService.save(badgeAwardUpvoteEvent);
  }

  @Test
  void testFollowSetsEvent() throws IOException, InterruptedException {
    final String FOLLOW_SETS_EVENT = "FOLLOW_SETS_EVENT";
    final IdentifierTag followSetsIdentifierTag = new IdentifierTag(FOLLOW_SETS_EVENT);

    FollowSetsEvent followSetsEvent = new FollowSetsEvent(
        identity,
        reputationRecipientPublicKey,
        followSetsIdentifierTag,
        relay,
        List.of(badgeAwardUpvoteEvent));

    String aimg5557 = "ws://localhost:5557";
    new AfterimageMeshRelayService(aimg5557)
        .send(
            new EventMessage(
                followSetsEvent),
            new TestSubscriber<>());

    TimeUnit.MILLISECONDS.sleep(5000);

//      then send RELAY SETS 5557 awareness to 5556...
    String aimg5556 = "ws://localhost:5556";
    new AfterimageMeshRelayService(aimg5556)
        .send(
            new EventMessage(
//      ...announcing 5557               
                createRelaysSetsEventMessage(aimg5557)),
            new TestSubscriber<>());

    TimeUnit.MILLISECONDS.sleep(5000);

//      sent REP request to 5556
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_A = new TestSubscriber<>();
    final AfterimageMeshRelayService afterimageRepRequestClient = new AfterimageMeshRelayService(aimg5556);
    afterimageRepRequestClient.send(
        createAfterImageReqMessage(Factory.generateRandomHex64String(), reputationRecipientPublicKey),
        afterImageEventsSubscriber_A);

    TimeUnit.MILLISECONDS.sleep(100);

    log.debug("afterimage returned superconductor events:");
    List<BaseMessage> items_3 = afterImageEventsSubscriber_A.getItems();
    log.debug("  {}", items_3);

    List<EventIF> returnedReqGenericEvents_2 = getGenericEvents(items_3);

    assert ("2".equals(returnedReqGenericEvents_2.getFirst().getContent()));
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

  private BaseEvent createRelaysSetsEventMessage(String uri) {
    return new RelaySetsEvent(
        afterimageInstanceIdentity,
        relaySetsIdentifierTag,
        "Kind.RELAY_SETS",
        new RelayTag(
            new Relay(uri)));
  }

  private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
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
