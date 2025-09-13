//package com.prosilion.afterimage.service.reactive;
//
//import com.prosilion.afterimage.util.AfterimageMeshRelayService;
//import com.prosilion.afterimage.util.Factory;
//import com.prosilion.afterimage.util.TestSubscriber;
//import com.prosilion.nostr.NostrException;
//import com.prosilion.nostr.enums.Kind;
//import com.prosilion.nostr.event.BadgeDefinitionEvent;
//import com.prosilion.nostr.event.BaseEvent;
//import com.prosilion.nostr.event.EventIF;
//import com.prosilion.nostr.event.RelaySetsEvent;
//import com.prosilion.nostr.event.SearchRelaysListEvent;
//import com.prosilion.nostr.event.internal.Relay;
//import com.prosilion.nostr.filter.Filters;
//import com.prosilion.nostr.filter.event.KindFilter;
//import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
//import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
//import com.prosilion.nostr.message.BaseMessage;
//import com.prosilion.nostr.message.EventMessage;
//import com.prosilion.nostr.message.ReqMessage;
//import com.prosilion.nostr.tag.PubKeyTag;
//import com.prosilion.nostr.tag.RelayTag;
//import com.prosilion.nostr.user.Identity;
//import com.prosilion.nostr.user.PublicKey;
//import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
//import java.io.File;
//import java.io.IOException;
//import java.security.NoSuchAlgorithmException;
//import java.util.List;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.MethodOrderer;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestMethodOrder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.lang.NonNull;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.ActiveProfiles;
//import org.testcontainers.containers.ComposeContainer;
//import org.testcontainers.containers.wait.strategy.Wait;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@Slf4j
//@TestMethodOrder(MethodOrderer.MethodName.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
//@ActiveProfiles("test")
//@Testcontainers
//@EmbeddedRedisStandalone
//@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
//public class FollowSetsEventIT {
//
//  public static final String SUPERCONDUCTOR_AFTERIMAGE = "superconductor-afterimage";
//  public static final String AFTERIMAGE_TEST = "afterimage-app-dev_ws-test";
//
//  @Container
//  private static final ComposeContainer DOCKER_COMPOSE_CONTAINER = new ComposeContainer(
//      new File("src/test/resources/superconductor-docker-compose/superconductor-docker-compose-dev-test-ws.yml"))
//      .withExposedService(SUPERCONDUCTOR_AFTERIMAGE, 5555)
//      .withRemoveVolumes(true);
//
//  @Container
//  private static final ComposeContainer AFTERIMAGE_COMPOSE_CONTAINER = new ComposeContainer(
//      new File("src/test/resources/afterimage-docker-compose/afterimage-docker-compose-dev-test-ws.yml"))
//      .withExposedService(AFTERIMAGE_TEST, 5557)
//      .withRemoveVolumes(true);
//
//  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
//  private final Identity afterimageInstanceIdentity;
//  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;
//  private final String superconductorRelayUri;
//  private final String afterimageRelayUri;
//  private final String afterimageDockerRelayUri;
//
//  @BeforeEach
//  public void setUp() {
//    log.info("BeforeEach DOCKER_COMPOSE_CONTAINER Wait.forHealthcheck()....");
//    DOCKER_COMPOSE_CONTAINER.waitingFor(SUPERCONDUCTOR_AFTERIMAGE, Wait.forHealthcheck());
//    log.info("... done BeforeEach DOCKER_COMPOSE_CONTAINER Wait.forHealthcheck()");
//
//    log.info("BeforeEach AFTERIMAGE_COMPOSE_CONTAINER Wait.forHealthcheck()....");
//    AFTERIMAGE_COMPOSE_CONTAINER.waitingFor(AFTERIMAGE_TEST, Wait.forHealthcheck());
//    log.info("... done BeforeEach AFTERIMAGE_COMPOSE_CONTAINER Wait.forHealthcheck()");
//  }
//
//  @BeforeAll
//  static void beforeAll() {
//    log.info("BeforeAll DOCKER_COMPOSE_CONTAINER.start()....");
//    DOCKER_COMPOSE_CONTAINER.start();
//    log.info("... done BeforeAll DOCKER_COMPOSE_CONTAINER.start()");
//
//    log.info("BeforeAll AFTERIMAGE_COMPOSE_CONTAINER.start()....");
//    AFTERIMAGE_COMPOSE_CONTAINER.start();
//    log.info("... done BeforeAll AFTERIMAGE_COMPOSE_CONTAINER.start()");
//  }
//
//  @Autowired
//  public FollowSetsEventIT(
//      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUri,
//      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri,
//      @NonNull @Value("${afterimage-docker.relay.url}") String afterimageDockerRelayUri,
//      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent,
//      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent,
//      @NonNull Identity afterimageInstanceIdentity) {
//    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;
//    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
//    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
//    this.superconductorRelayUri = superconductorRelayUri;
//    this.afterimageRelayUri = afterimageRelayUri;
//    this.afterimageDockerRelayUri = afterimageDockerRelayUri;
//  }
//
//  @Test
//  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException, NoSuchAlgorithmException, InterruptedException {
//    final AfterimageMeshRelayService afterimageSubscriberCheckClient = new AfterimageMeshRelayService(afterimageRelayUri);
//    final Identity upvotedUser = Identity.generateRandomIdentity();
//
//    TestSubscriber<BaseMessage> reputationRequestSubscriberCheck = new TestSubscriber<>();
//    afterimageSubscriberCheckClient.send(
//        createAfterImageReqMessage(
//            Factory.generateRandomHex64String(),
//            upvotedUser.getPublicKey()),
//        reputationRequestSubscriberCheck);
//
//    //  test initial aImg events state, should have zero reputation events for upvotedUser
//    log.debug("afterimage initial events:");
//    List<BaseMessage> initialItems = reputationRequestSubscriberCheck.getItems();
//    afterimageSubscriberCheckClient.closeSocket();
//    log.debug("  {}", initialItems);
//
//    List<EventIF> initialEvents = getGenericEvents(initialItems);
//    assertEquals(0, initialEvents.size());
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
//  private ReqMessage createAfterImageReqMessage(String subscriberId, PublicKey upvotedUserPublicKey) {
//    return new ReqMessage(
//        subscriberId,
//        new Filters(
//            new KindFilter(
//                Kind.BADGE_AWARD_EVENT),
//            new ReferencedPublicKeyFilter(
//                new PubKeyTag(
//                    upvotedUserPublicKey)),
//            new IdentifierTagFilter(
//                reputationBadgeDefinitionEvent.getIdentifierTag())));
//  }
//
//  private BaseEvent createSearchRelaysListEventMessage() throws NoSuchAlgorithmException {
//    return new SearchRelaysListEvent(
//        afterimageInstanceIdentity,
//        "Kind.SEARCH_RELAYS_LIST",
//        new RelayTag(
//            new Relay(superconductorRelayUri)));
//  }
//
//  private BaseEvent createFollowSetsEventMessage() throws NoSuchAlgorithmException {
//    return new RelaySetsEvent(
//        afterimageInstanceIdentity,
//        List.of(new RelayTag(new Relay(afterimageDockerRelayUri))),
//        "Kind.RELAY_SETS");
//  }
//}
