package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.RelaySetsEvent;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.lang.NonNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Testcontainers
@EmbeddedRedisStandalone
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class RelaySetsIT {

  public static final String SUPERCONDUCTOR_AFTERIMAGE = "superconductor-afterimage";
  public static final String AFTERIMAGE = "afterimage-app";

  @Container
  private static final ComposeContainer AIMG_DOCKER_COMPOSE_CONTAINER = new ComposeContainer(
      new File("src/test/resources/afterimage-docker-compose/afterimage-docker-compose-dev-test-ws.yml"))
      .withExposedService(SUPERCONDUCTOR_AFTERIMAGE, 5555)
      .withExposedService(AFTERIMAGE, 5556)
      .withRemoveVolumes(true);

  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
  private final Identity afterimageInstanceIdentity;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;
  private final String superconductorRelayUri;
  private final String afterimageDockerRelayUri;
  private final String afterimageRelayUri;

  @BeforeEach
  public void setUp() {
    log.info("BeforeEach DOCKER_COMPOSE_CONTAINER Wait.forHealthcheck()....");
    AIMG_DOCKER_COMPOSE_CONTAINER.waitingFor(SUPERCONDUCTOR_AFTERIMAGE, Wait.forHealthcheck());
    AIMG_DOCKER_COMPOSE_CONTAINER.waitingFor(AFTERIMAGE, Wait.forHealthcheck());
    log.info("... done BeforeEach DOCKER_COMPOSE_CONTAINER Wait.forHealthcheck()");
  }

  @BeforeAll
  static void beforeAll() {
    log.info("BeforeAll DOCKER_COMPOSE_CONTAINER.start()....");
//    SC_DOCKER_COMPOSE_CONTAINER.start();
    AIMG_DOCKER_COMPOSE_CONTAINER.start();
    log.info("... done BeforeAll DOCKER_COMPOSE_CONTAINER.start()");
  }

  @Autowired
  public RelaySetsIT(
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUri,
      @NonNull @Value("${afterimage-docker.relay.url}") String afterimageDockerRelayUri,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri,
      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent,
      @NonNull Identity afterimageInstanceIdentity) {
    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.superconductorRelayUri = superconductorRelayUri;
    this.afterimageDockerRelayUri = afterimageDockerRelayUri;
    this.afterimageRelayUri = afterimageRelayUri;
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException, NoSuchAlgorithmException, InterruptedException {
    final AfterimageMeshRelayService afterimageSubscriberCheckClient = new AfterimageMeshRelayService(afterimageRelayUri);
    final Identity authorIdentity = Identity.generateRandomIdentity();
    final Identity upvotedUser = Identity.generateRandomIdentity();

    TestSubscriber<BaseMessage> reputationRequestSubscriberCheck = new TestSubscriber<>();
    afterimageSubscriberCheckClient.send(
        createReputationReqMessage(
            Factory.generateRandomHex64String(),
            upvotedUser.getPublicKey()),
        reputationRequestSubscriberCheck);

    //  test initial aImg events state, should have zero reputation events for upvotedUser
    log.debug("afterimage initial events:");
    List<BaseMessage> initialItems = reputationRequestSubscriberCheck.getItems();
    afterimageSubscriberCheckClient.closeSocket();
    log.debug("  {}", initialItems);

    List<EventIF> initialEvents = getGenericEvents(initialItems);
    assertEquals(0, initialEvents.size());

//  submit SC vote(s)  
    BadgeAwardUpvoteEvent event = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        upvoteBadgeDefinitionEvent);

    BadgeAwardUpvoteEvent event_new = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        upvoteBadgeDefinitionEvent);

    assertEquals(event.getPublicKey().toHexString(), authorIdentity.getPublicKey().toHexString());

//  submit upvote event to SC
    TestSubscriber<OkMessage> okMessageSubscriber_sc_1 = new TestSubscriber<>();
    new AfterimageMeshRelayService(superconductorRelayUri).send(new EventMessage(event), okMessageSubscriber_sc_1);

    TimeUnit.MILLISECONDS.sleep(500);

    List<OkMessage> items_1 = okMessageSubscriber_sc_1.getItems();
    assertEquals(true, items_1.getFirst().getFlag());

    TestSubscriber<OkMessage> okMessageSubscriber_sc_2 = new TestSubscriber<>();
    new AfterimageMeshRelayService(superconductorRelayUri).send(new EventMessage(event_new), okMessageSubscriber_sc_2);

    TimeUnit.MILLISECONDS.sleep(500);

    List<OkMessage> items_2 = okMessageSubscriber_sc_2.getItems();
    assertEquals(true, items_2.getFirst().getFlag());

    TestSubscriber<OkMessage> okMessageSubscriber_aImg_1 = new TestSubscriber<>();
    AfterimageMeshRelayService afterimageMeshRelayService = new AfterimageMeshRelayService(afterimageDockerRelayUri);
    afterimageMeshRelayService
        .send(
            new EventMessage(
                createSearchRelaysListEventMessage(superconductorRelayUri)),
            okMessageSubscriber_aImg_1);

    List<OkMessage> items_aImg = okMessageSubscriber_aImg_1.getItems();
    assertEquals(true, items_aImg.getFirst().getFlag());
    afterimageMeshRelayService.closeSocket();
    TimeUnit.MILLISECONDS.sleep(2000);

//    TODO: check aImgDocker reputation, should have "2"

    TestSubscriber<BaseMessage> aImgDockerEventsSubscriber = new TestSubscriber<>();
    final AfterimageMeshRelayService aImgDockerEventClient = new AfterimageMeshRelayService(afterimageDockerRelayUri);
    aImgDockerEventClient.send(
        createReputationReqMessage(Factory.generateRandomHex64String(), upvotedUser.getPublicKey()),
        aImgDockerEventsSubscriber);

    TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("afterimage returned superconductor events:");
    List<BaseMessage> items_aImg_Docker = aImgDockerEventsSubscriber.getItems();
    log.debug("  {}", items_aImg_Docker);

    List<EventIF> returnedEventsAImg = getGenericEvents(items_aImg_Docker);
//    assertEquals(1, returnedEventsAImg.size());
//    assertEquals("2", returnedEventsAImg.getFirst().getContent());

//    submit RelaySets event to aImg containing aImg docker as a RelaySets source 
    new AfterimageMeshRelayService(afterimageRelayUri)
        .send(
            new EventMessage(
                createRelaysSetsEventMessage(afterimageDockerRelayUri)),
            new TestSubscriber<>());

    TimeUnit.MILLISECONDS.sleep(1000);

//  query Aimg for REPUTATION event existence
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_A = new TestSubscriber<>();
    final AfterimageMeshRelayService afterimageRepRequestClient = new AfterimageMeshRelayService(afterimageRelayUri);
    afterimageRepRequestClient.send(
        createReputationReqMessage(Factory.generateRandomHex64String(), upvotedUser.getPublicKey()),
        afterImageEventsSubscriber_A);

    TimeUnit.MILLISECONDS.sleep(100);

    log.debug("afterimage returned superconductor events:");
    List<BaseMessage> items_3 = afterImageEventsSubscriber_A.getItems();
    log.debug("  {}", items_3);

    List<EventIF> returnedEvents = getGenericEvents(items_3);
    assertEquals(1, returnedEvents.size());
    assertEquals("2", returnedEvents.getFirst().getContent());
  }

  private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
  }

  private ReqMessage createReputationReqMessage(String subscriberId, PublicKey upvotedUserPublicKey) {
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

  private BaseEvent createRelaysSetsEventMessage(String uri) throws NoSuchAlgorithmException {
    return new RelaySetsEvent(
        afterimageInstanceIdentity,
        "Kind.RELAY_SETS",
        new RelayTag(
            new Relay(uri)));
  }

  private BaseEvent createSearchRelaysListEventMessage(String uri) throws NoSuchAlgorithmException {
    String tempUrl = "ws://superconductor-afterimage:5555";
    return new SearchRelaysListEvent(
        afterimageInstanceIdentity,
        Stream.of(tempUrl).map(relayString ->
            new RelayTag(new Relay(relayString))).toList(),
        "Kind.SEARCH_RELAYS_LIST");
  }
}
