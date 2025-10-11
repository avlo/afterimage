package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.calculator.UnitReputationCalculator;
import com.prosilion.afterimage.config.TestcontainersConfig;
import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.RelaySetsEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
  public static final Identity authorIdentity = Identity.generateRandomIdentity();
  public static final PublicKey UPVOTED_USER = Identity.create("1231231231231231231231231231231231231231231231231231231231231231").getPublicKey();
  public static final String EVENT_ID_666 = "6666666666666666666666666666666666666666666666666666666666666666";
  public static final String EVENT_ID_777 = "7777777777777777777777777777777777777777777777777777777777777777";
  BadgeDefinitionEvent reputationBadgeDefinitionEvent;
  Identity afterimageInstanceIdentity;
  FollowSetsEvent followSetsEvent;

  @Autowired
  public FollowSetsIT(
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent,
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull String afterimageRelayUrl) {
    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
    System.out.print("voteReceiverPubkey:\n  ");
    System.out.println(UPVOTED_USER);
    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.followSetsEvent = new FollowSetsEvent(
        authorIdentity,
        UPVOTED_USER,
        new IdentifierTag(
            UnitReputationCalculator.class.getCanonicalName()),
        List.of(
            createPair(EVENT_ID_666, afterimageRelayUrl),
            createPair(EVENT_ID_777, afterimageRelayUrl)),
        UnitReputationCalculator.class.getName());
  }

  @Test
  void testFollowSetsEvent() throws IOException, InterruptedException {
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
        createAfterImageReqMessage(Factory.generateRandomHex64String(), UPVOTED_USER),
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
                reputationBadgeDefinitionEvent.getIdentifierTag())));
  }

  private BaseEvent createRelaysSetsEventMessage(String uri) {
    return new RelaySetsEvent(
        afterimageInstanceIdentity,
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

  private FollowSetsEvent.EventTagAddressTagPair createPair(String eventId, String afterimageRelayUrl) {
    return new FollowSetsEvent.EventTagAddressTagPair(
        new EventTag(
            eventId,
            afterimageRelayUrl),
        new AddressTag(
            Kind.BADGE_AWARD_EVENT,
            authorIdentity.getPublicKey(),
            new IdentifierTag(
                SuperconductorKindType.UNIT_UPVOTE
                    .getName())));
  }
}
