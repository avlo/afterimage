package com.prosilion.afterimage.config;

import com.prosilion.afterimage.AfterimageApplication;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
//@Profile("!test")
public class LocalDevApplication {
  public static final Identity authorIdentity = Identity.generateRandomIdentity();
  public static final PublicKey UPVOTED_USER = Identity.create("1231231231231231231231231231231231231231231231231231231231231231").getPublicKey();
  public static final String EVENT_ID_666 = "6666666666666666666666666666666666666666666666666666666666666666";
  public static final String EVENT_ID_777 = "7777777777777777777777777777777777777777777777777777777777777777";

  public static void main(String[] args) {
    SpringApplication.from(AfterimageApplication::main).with(TestcontainersConfig.class).run(args);
  }

  //  @Bean
//  public FollowSetsDataLoaderRedis followSetsDataLoaderRedis(
//      @NonNull BadgeDefinitionReputationEvent reputationBadgeDefinitionEvent,
//      @NonNull Identity afterimageInstanceIdentity,
//      @NonNull String afterimageRelayUrl) {
//    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
//    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
//    System.out.print("voteReceiverPubkey:\n  ");
//    System.out.println(UPVOTED_USER);
//    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
//    System.out.println("VOTE_RECEIVER_PUBKEY-----VOTE_RECEIVER_PUBKEY");
//    return new FollowSetsDataLoaderRedis(
//        reputationBadgeDefinitionEvent,
//        new FollowSetsEvent(
//            authorIdentity,
//            UPVOTED_USER,
//            reputationBadgeDefinitionEvent.getIdentifierTag(),
//            new Relay(afterimageRelayUrl),
//            List.of(
//                createPair(EVENT_ID_666, afterimageRelayUrl),
//                createPair(EVENT_ID_777, afterimageRelayUrl)),
//            DynamicReputationCalculator.class.getName()),
//        afterimageInstanceIdentity);
//  }
//
//  private static EventTagAddressTagPair createPair(String eventId, String afterimageRelayUrl) {
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
//
//  public static class FollowSetsDataLoaderRedis implements DataLoaderRedisIF {
//    private final FollowSetsEvent followSetsEvent;
//    private final Identity afterimageInstanceIdentity;
//    private final BadgeDefinitionReputationEvent reputationBadgeDefinitionEvent;
//
//    public FollowSetsDataLoaderRedis(
//        @NonNull BadgeDefinitionReputationEvent reputationBadgeDefinitionEvent,
//        @NonNull FollowSetsEvent followSetsEvent,
//        @NonNull Identity afterimageInstanceIdentity) {
//      this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
//      this.followSetsEvent = followSetsEvent;
//      this.afterimageInstanceIdentity = afterimageInstanceIdentity;
//    }
//
//    private BaseEvent createRelaysSetsEventMessage(String uri) {
//      return new RelaySetsEvent(
//          afterimageInstanceIdentity,
//          "Kind.RELAY_SETS",
//          new RelayTag(
//              new Relay(uri)));
//    }
//
//    @Override
//    public void run(String... args) throws IOException, InterruptedException {
////    setup: submit follow sets (i.e., REP history event) to 5557
//      String aimg5557 = "ws://localhost:5557";
//      new AfterimageMeshRelayService(aimg5557)
//          .send(
//              new EventMessage(
//                  followSetsEvent),
//              new TestSubscriber<>());
//
//      TimeUnit.MILLISECONDS.sleep(5000);
//
////      then send RELAY SETS 5557 awareness to 5556...
//      String aimg5556 = "ws://localhost:5556";
//      new AfterimageMeshRelayService(aimg5556)
//          .send(
//              new EventMessage(
////      ...announcing 5557               
//                  createRelaysSetsEventMessage(aimg5557)),
//              new TestSubscriber<>());
//
//      TimeUnit.MILLISECONDS.sleep(5000);
//
////      sent REP request to 5556
//      TestSubscriber<BaseMessage> afterImageEventsSubscriber_A = new TestSubscriber<>();
//      final AfterimageMeshRelayService afterimageRepRequestClient = new AfterimageMeshRelayService(aimg5556);
//      afterimageRepRequestClient.send(
//          createAfterImageReqMessage(Factory.generateRandomHex64String(), UPVOTED_USER, Kind.BADGE_AWARD_EVENT),
//          afterImageEventsSubscriber_A);
//
//      TimeUnit.MILLISECONDS.sleep(100);
//
//      log.debug("afterimage returned superconductor events:");
//      List<BaseMessage> items_3 = afterImageEventsSubscriber_A.getItems();
//      log.debug("  {}", items_3);
//
//      List<EventIF> returnedReqGenericEvents_2 = getGenericEvents(items_3);
//
//      assert ("2".equals(returnedReqGenericEvents_2.getFirst().getContent()));
//    }
//
//    private ReqMessage createAfterImageReqMessage(String subscriberId, PublicKey upvotedUserPublicKey, Kind kind) {
//      return new ReqMessage(
//          subscriberId,
//          new Filters(
//              new KindFilter(kind),
//              new ReferencedPublicKeyFilter(
//                  new PubKeyTag(
//                      upvotedUserPublicKey)),
//              new IdentifierTagFilter(
//                  reputationBadgeDefinitionEvent.getIdentifierTag())));
//    }
//
//    private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
//      return returnedBaseMessages.stream()
//          .filter(EventMessage.class::isInstance)
//          .map(EventMessage.class::cast)
//          .map(EventMessage::getEvent)
//          .toList();
//    }
//  }
}
