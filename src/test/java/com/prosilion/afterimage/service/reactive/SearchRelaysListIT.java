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
public class SearchRelaysListIT {

  public static final String SUPERCONDUCTOR_AFTERIMAGE = "superconductor-afterimage";

  @Container
  private static final ComposeContainer DOCKER_COMPOSE_CONTAINER = new ComposeContainer(
      new File("src/test/resources/superconductor-docker-compose/superconductor-docker-compose-dev-test-ws.yml"))
      .withExposedService(SUPERCONDUCTOR_AFTERIMAGE, 5555)
      .withRemoveVolumes(true);
  
  @Container
  private static final ComposeContainer DOCKER_COMPOSE_CONTAINER_2 = new ComposeContainer(
      new File("src/test/resources/superconductor-docker-compose-2/superconductor-docker-compose-dev-test-ws.yml"))
      .withExposedService(SUPERCONDUCTOR_AFTERIMAGE, 5555)
      .withRemoveVolumes(true);

  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
  private final Identity afterimageInstanceIdentity;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;
  private final String superconductorRelayUri;
  private final String superconductorRelayUri_2;
  private final String afterimageRelayUri;

  @BeforeEach
  public void setUp() {
    log.info("BeforeEach DOCKER_COMPOSE_CONTAINER Wait.forHealthcheck()....");
    DOCKER_COMPOSE_CONTAINER.waitingFor(SUPERCONDUCTOR_AFTERIMAGE, Wait.forHealthcheck());
    DOCKER_COMPOSE_CONTAINER_2.waitingFor(SUPERCONDUCTOR_AFTERIMAGE, Wait.forHealthcheck());
    log.info("... done BeforeEach DOCKER_COMPOSE_CONTAINER Wait.forHealthcheck()");
  }

  @BeforeAll
  static void beforeAll() {
    log.info("BeforeAll DOCKER_COMPOSE_CONTAINER.start()....");
    DOCKER_COMPOSE_CONTAINER.start();
    DOCKER_COMPOSE_CONTAINER_2.start();
    log.info("... done BeforeAll DOCKER_COMPOSE_CONTAINER.start()");
  }

  @Autowired
  public SearchRelaysListIT(
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUri,
      @NonNull @Value("${superconductor.relay.url.2}") String superconductorRelayUri_2,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri,
      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent,
      @NonNull Identity afterimageInstanceIdentity) {

    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.superconductorRelayUri = superconductorRelayUri;
    this.superconductorRelayUri_2 = superconductorRelayUri_2;
    this.afterimageRelayUri = afterimageRelayUri;
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException, NoSuchAlgorithmException, InterruptedException {
    final AfterimageMeshRelayService afterimageSubscriberCheckClient = new AfterimageMeshRelayService(afterimageRelayUri);
    final Identity upvotedUser = Identity.generateRandomIdentity();

    TestSubscriber<BaseMessage> reputationRequestSubscriberCheck = new TestSubscriber<>();
    afterimageSubscriberCheckClient.send(
        createAfterImageReqMessage(
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
    final Identity authorIdentity = Identity.generateRandomIdentity();

    BadgeAwardUpvoteEvent event = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        upvoteBadgeDefinitionEvent);

    assertEquals(event.getPublicKey().toHexString(), authorIdentity.getPublicKey().toHexString());

//  submit upvote event to SC
    TestSubscriber<OkMessage> okMessageSubscriber_1 = new TestSubscriber<>();
    new AfterimageMeshRelayService(superconductorRelayUri).send(new EventMessage(event), okMessageSubscriber_1);

    TestSubscriber<OkMessage> okMessageSubscriber_2 = new TestSubscriber<>();
    new AfterimageMeshRelayService(superconductorRelayUri_2).send(new EventMessage(event), okMessageSubscriber_2);
    
    TimeUnit.MILLISECONDS.sleep(50);

    List<OkMessage> items_1 = okMessageSubscriber_1.getItems();
    assertEquals(true, items_1.getFirst().getFlag());

    List<OkMessage> items_2 = okMessageSubscriber_2.getItems();
    assertEquals(true, items_2.getFirst().getFlag());

//  submit search relays list event to aImg w/ SC url, should:
//    1. get upvote event from SC
//    2. create REPUTATION event in aImg
    new AfterimageMeshRelayService(afterimageRelayUri)
        .send(
            new EventMessage(
                createSearchRelaysListEventMessage(superconductorRelayUri)),
            new TestSubscriber<>());

    TimeUnit.MILLISECONDS.sleep(1000);

    new AfterimageMeshRelayService(afterimageRelayUri)
        .send(
            new EventMessage(
                createSearchRelaysListEventMessage(superconductorRelayUri_2)),
            new TestSubscriber<>());

    TimeUnit.MILLISECONDS.sleep(1000);

//    query Aimg for above REPUTATION event
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_A = new TestSubscriber<>();
    final AfterimageMeshRelayService afterimageRepRequestClient = new AfterimageMeshRelayService(afterimageRelayUri);
    afterimageRepRequestClient.send(
        createAfterImageReqMessage(Factory.generateRandomHex64String(), upvotedUser.getPublicKey()),
        afterImageEventsSubscriber_A);

    TimeUnit.MILLISECONDS.sleep(100);

    log.debug("afterimage returned superconductor events:");
    List<BaseMessage> items_3 = afterImageEventsSubscriber_A.getItems();
    log.debug("  {}", items_3);

    List<EventIF> returnedReqGenericEvents_2 = getGenericEvents(items_3);

    assertEquals("1", returnedReqGenericEvents_2.getFirst().getContent());
    assertEquals(returnedReqGenericEvents_2.getFirst().getPublicKey().toHexString(), afterimageInstanceIdentity.getPublicKey().toHexString());
    assertEquals(returnedReqGenericEvents_2.getFirst().getKind(), event.getKind());

    afterimageRepRequestClient.closeSocket();
  }

  private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
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

  private BaseEvent createSearchRelaysListEventMessage(String uri) throws NoSuchAlgorithmException {
    return new SearchRelaysListEvent(
        afterimageInstanceIdentity,
        "Kind.SEARCH_RELAYS_LIST",
        new RelayTag(
            new Relay(uri)));
  }
}
