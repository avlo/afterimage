package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.EventServiceIF;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Testcontainers
@EmbeddedRedisStandalone
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class SuperconductorEventThenAfterimageReqIT {

  public static final String SUPERCONDUCTOR_AFTERIMAGE = "superconductor-afterimage";

  @Container
  private static final ComposeContainer DOCKER_COMPOSE_CONTAINER = new ComposeContainer(
      new File("src/test/resources/superconductor-docker-compose/superconductor-docker-compose-dev-test-ws.yml"))
      .withExposedService(SUPERCONDUCTOR_AFTERIMAGE, 5555)
      .withRemoveVolumes(true);

  private final EventServiceIF eventService;
  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
  private final PublicKey afterimageInstancePublicKey;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;
  private final String superconductorRelayUri;
  private final String afterimageRelayUri;

  @BeforeEach
  public void setUp() {
    log.info("BeforeEach DOCKER_COMPOSE_CONTAINER Wait.forHealthcheck()....");
    DOCKER_COMPOSE_CONTAINER.waitingFor(SUPERCONDUCTOR_AFTERIMAGE, Wait.forHealthcheck());
    log.info("... done BeforeEach DOCKER_COMPOSE_CONTAINER Wait.forHealthcheck()");
  }

  @BeforeAll
  static void beforeAll() {
    log.info("BeforeAll DOCKER_COMPOSE_CONTAINER.start()....");
    DOCKER_COMPOSE_CONTAINER.start();
    log.info("... done BeforeAll DOCKER_COMPOSE_CONTAINER.start()");
  }

  @Autowired
  public SuperconductorEventThenAfterimageReqIT(
      @NonNull EventServiceIF eventService,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUri,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri,
      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent,
      @NonNull Identity afterimageInstanceIdentity) {

    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.afterimageInstancePublicKey = afterimageInstanceIdentity.getPublicKey();
    this.eventService = eventService;
    this.superconductorRelayUri = superconductorRelayUri;
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

//  begin SC votes submissions  

    final Identity authorIdentity = Identity.generateRandomIdentity();
    final AfterimageMeshRelayService superconductorRelayReactiveClient = new AfterimageMeshRelayService(superconductorRelayUri);

    BadgeAwardUpvoteEvent event = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        upvoteBadgeDefinitionEvent);

//    GenericEventKindTypeIF badgeAwardUpvoteEvent_1 =
//        new GenericDocumentKindTypeDto(
//            event,
//            SuperconductorKindType.UNIT_UPVOTE)
//            .convertBaseEventToGenericEventKindTypeIF();

    assertEquals(event.getPublicKey().toHexString(), authorIdentity.getPublicKey().toHexString());

//    submit Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(event), okMessageSubscriber_1);

    TimeUnit.MILLISECONDS.sleep(50);

    List<OkMessage> items_1 = okMessageSubscriber_1.getItems();
    assertEquals(true, items_1.getFirst().getFlag());

    //    submit Req for above event to superconductor

    TestSubscriber<BaseMessage> superconductorEventsSubscriber_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(Factory.generateRandomHex64String()),
        superconductorEventsSubscriber_1);

    TimeUnit.MILLISECONDS.sleep(50);

    log.debug("retrieved afterimage events:");
    List<EventIF> returnedSuperconductorEvents =
        getGenericEvents(
            superconductorEventsSubscriber_1.getItems());

    assertEquals(returnedSuperconductorEvents.getFirst().getId(), event.getId());
    assertEquals(returnedSuperconductorEvents.getFirst().getContent(), event.getContent());
    assertEquals(returnedSuperconductorEvents.getFirst().getPublicKey().toHexString(), event.getPublicKey().toHexString());
    assertEquals(returnedSuperconductorEvents.getFirst().getKind(), event.getKind());

//    save SC result to Aimg
    returnedSuperconductorEvents.forEach(gev ->
        eventService.processIncomingEvent(
            new EventMessage(createGenericEventRecord(gev))));

//    query Aimg for above event
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_A = new TestSubscriber<>();
    final AfterimageMeshRelayService afterimageRepRequestClient = new AfterimageMeshRelayService(afterimageRelayUri);
    afterimageRepRequestClient.send(
        createAfterImageReqMessage(Factory.generateRandomHex64String(), upvotedUser.getPublicKey()),
        afterImageEventsSubscriber_A);

    TimeUnit.MILLISECONDS.sleep(50);

    log.debug("afterimage returned superconductor events:");
    List<BaseMessage> items_2 = afterImageEventsSubscriber_A.getItems();
    log.debug("  {}", items_2);

    List<EventIF> returnedReqGenericEvents_2 = getGenericEvents(items_2);

    assertEquals("1", returnedReqGenericEvents_2.getFirst().getContent());
    assertEquals(returnedReqGenericEvents_2.getFirst().getPublicKey().toHexString(), afterimageInstancePublicKey.toHexString());
    assertEquals(returnedReqGenericEvents_2.getFirst().getKind(), event.getKind());

    superconductorRelayReactiveClient.closeSocket();
    afterimageRepRequestClient.closeSocket();
  }

  @Test
//  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  void testB_SuperconductorTwoEventsThenAfterimageReq() throws IOException, NostrException, NoSuchAlgorithmException, InterruptedException {
    final Identity upvotedUser = Identity.generateRandomIdentity();
    final Identity authorIdentity = Identity.generateRandomIdentity();
    final AfterimageMeshRelayService superconductorRelayReactiveClient = new AfterimageMeshRelayService(superconductorRelayUri);
    final AfterimageMeshRelayService afterimageMeshRelayService = new AfterimageMeshRelayService(afterimageRelayUri);

    //    create & submit subscriber's first Event to superconductor
    BadgeAwardUpvoteEvent upvote_1 = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        upvoteBadgeDefinitionEvent);
//    GenericEventKindTypeIF upvote_1 = new GenericDocumentKindTypeDto(
//        upvote_1,
//        SuperconductorKindType.UNIT_UPVOTE).convertBaseEventToGenericEventKindTypeIF();

    TestSubscriber<OkMessage> okMessageSubscriber_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(upvote_1), okMessageSubscriber_1);
    TimeUnit.MILLISECONDS.sleep(50);

    List<OkMessage> items1 = okMessageSubscriber_1.getItems();
    TimeUnit.MILLISECONDS.sleep(50);

    assertEquals(true, items1.getFirst().getFlag());
    log.debug("received 1of2 OkMessage...");

//    create & submit subscriber's second Event to superconductor
    BadgeAwardUpvoteEvent upvote_2 = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        upvoteBadgeDefinitionEvent);
//    GenericEventKindTypeIF upvote_2 = new GenericDocumentKindTypeDto(
//        upvote_2,
//        SuperconductorKindType.UNIT_UPVOTE).convertBaseEventToGenericEventKindTypeIF();

//    okMessageSubscriber_1.dispose();
    TestSubscriber<OkMessage> okMessageSubscriber_2 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(upvote_2), okMessageSubscriber_2);
    TimeUnit.MILLISECONDS.sleep(50);

    List<OkMessage> items = okMessageSubscriber_2.getItems();
    assertEquals(true, items.getFirst().getFlag());
    log.debug("received 2of2 OkMessage...");

////    create & submit subscriber's third Event to superconductor
//    BadgeAwardUpvoteEvent textNoteEvent_3 = new BadgeAwardUpvoteEvent(authorIdentity, upvotedUser.getPublicKey(), upvoteBadgeDefinitionEvent);
//    GenericEventKindTypeIF genericEventKindIF3 = new GenericDocumentKindTypeDto(textNoteEvent_3, SuperconductorKindType.UNIT_UPVOTE).convertBaseEventToGenericEventKindTypeIF();
//
////    okMessageSubscriber_1.dispose();
//    TestSubscriber<OkMessage> okMessageSubscriber_3 = new TestSubscriber<>();
//    superconductorRelayReactiveClient.send(new EventMessage(genericEventKindIF3), okMessageSubscriber_3);
//    TimeUnit.MILLISECONDS.sleep(50);
//
//    List<OkMessage> items3 = okMessageSubscriber_2.getItems();
//    assertEquals(true, items3.getFirst().getFlag());
//    log.debug("received 2of3 OkMessage...");

// # --------------------- REQ -------------------    
//    submit votes Req to superconductor

    TestSubscriber<BaseMessage> superConductorEventsSubscriber_W = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(Factory.generateRandomHex64String()), superConductorEventsSubscriber_W);


    List<EventIF> returnedVotesFromSc = getGenericEvents(
        superConductorEventsSubscriber_W.getItems());

    assertTrue(returnedVotesFromSc.stream().map(EventIF::getId).anyMatch(upvote_1.getId()::equals));
    assertTrue(returnedVotesFromSc.stream().map(EventIF::getId).anyMatch(upvote_2.getId()::equals));
    assertTrue(returnedVotesFromSc.stream().map(EventIF::getPublicKey).map(PublicKey::toString).anyMatch(upvote_1.getPublicKey().toString()::equals));
    assertTrue(returnedVotesFromSc.stream().map(EventIF::getPublicKey).map(PublicKey::toString).anyMatch(upvote_2.getPublicKey().toString()::equals));
    assertTrue(returnedVotesFromSc.stream().map(EventIF::getKind).anyMatch(upvote_1.getKind()::equals));

//    save SC result to Aimg
    returnedVotesFromSc.forEach(event -> eventService.processIncomingEvent(new EventMessage(createGenericEventRecord(event))));

    TimeUnit.MILLISECONDS.sleep(250);

//    query Aimg for (as yet to be impl'd) reputation score event
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_V = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        createAfterImageReqMessage(Factory.generateRandomHex64String(), upvotedUser.getPublicKey()), afterImageEventsSubscriber_V);

    TimeUnit.MILLISECONDS.sleep(50);

    List<EventIF> returnedAfterImageEvents = getGenericEvents(
        afterImageEventsSubscriber_V.getItems());

    TimeUnit.MILLISECONDS.sleep(50);

//    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getId().equals(textNoteEvent_1.getId())));
    assertEquals(1, returnedAfterImageEvents.size());
    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getPublicKey().toHexString().equals(afterimageInstancePublicKey.toHexString())));
    assertEquals(returnedAfterImageEvents.getFirst().getKind(), upvote_1.getKind());
    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getKind().equals(upvote_1.getKind())));

    log.debug("returnedAfterImageEvents.size() {}", returnedAfterImageEvents.size());
    log.debug("------");
    log.debug("returnedAfterImageEvents:");
    returnedAfterImageEvents.forEach(a -> log.debug("{}\n----------\n", a.getContent()));
    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals("2")));

    superconductorRelayReactiveClient.closeSocket();
    afterimageMeshRelayService.closeSocket();
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

  private ReqMessage createSuperconductorReqMessage(String subscriberId) {
    return new ReqMessage(subscriberId,
        new Filters(
//            new ReferencedPublicKeyFilter(new PubKeyTag(upvotedUserPublicKey)),
            new KindFilter(Kind.BADGE_AWARD_EVENT)));
  }

  private GenericEventRecord createGenericEventRecord(EventIF event) {
    return new GenericEventRecord(
        event.getId(),
        event.getPublicKey(),
        event.getCreatedAt(),
        event.getKind(),
        event.getTags(),
        event.getContent(),
        event.getSignature());
  }
}
