package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.CanonicalAuthenticationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.CanonicalAuthenticationMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "superconductor.auth.event.kinds=SEARCH_RELAYS_LIST"
//    ,
//    "afterimage.relay.url=ws://localhost:5561",
//    "server.port=5561",
//    "spring.data.redis.port=6391",
//    "superconductor.auth.challenge-relay.url=ws://localhost:5561"
})
public class SearchRelaysListAuthSubmissionIT {
  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
  private final Identity afterimageInstanceIdentity;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;
  private final String superconductorRelayUrl;
  private final String superconductorRelayUrl_2;
  private final String afterimageRelayUri;

  @Autowired
  public SearchRelaysListAuthSubmissionIT(
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
      @NonNull @Value("${superconductor.relay.url.two}") String superconductorRelayUrl2,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri,
      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent,
      @NonNull Identity afterimageInstanceIdentity) {

    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.superconductorRelayUrl = superconductorRelayUrl;
    this.superconductorRelayUrl_2 = superconductorRelayUrl2;
    this.afterimageRelayUri = afterimageRelayUri;
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException, InterruptedException, NoSuchAlgorithmException {
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

//  create SC vote(s)  
    final Identity authorIdentity = Identity.generateRandomIdentity();

    BadgeAwardUpvoteEvent event = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        upvoteBadgeDefinitionEvent);

    BadgeAwardUpvoteEvent event_new = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        upvoteBadgeDefinitionEvent);

    assertEquals(event.getPublicKey().toHexString(), authorIdentity.getPublicKey().toHexString());

//  submit upvote events to SC
    TestSubscriber<OkMessage> okMessageSubscriber_sc_1 = new TestSubscriber<>();
    new AfterimageMeshRelayService(superconductorRelayUrl).send(new EventMessage(event), okMessageSubscriber_sc_1);
    TimeUnit.MILLISECONDS.sleep(50);
    List<OkMessage> items_1 = okMessageSubscriber_sc_1.getItems();
    assertEquals(true, items_1.getFirst().getFlag());

    TestSubscriber<OkMessage> okMessageSubscriber_sc_2 = new TestSubscriber<>();
    new AfterimageMeshRelayService(superconductorRelayUrl_2).send(new EventMessage(event_new), okMessageSubscriber_sc_2);
    TimeUnit.MILLISECONDS.sleep(50);
    List<OkMessage> items_2 = okMessageSubscriber_sc_2.getItems();
    assertEquals(true, items_2.getFirst().getFlag());

//  submit failing event
    TestSubscriber<OkMessage> rejectionClient = new TestSubscriber<>();
    final AfterimageMeshRelayService aImgSearchRelaysListRejectionSubscriber = new AfterimageMeshRelayService(afterimageRelayUri);

    aImgSearchRelaysListRejectionSubscriber.send(
        new EventMessage(
            createSearchRelaysListEventMessage(superconductorRelayUrl)),
        rejectionClient);

    TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("afterimage returned superconductor events:");
    OkMessage okRejectionMessage = rejectionClient.getItems().getFirst();
    log.debug("  {}", okRejectionMessage);

    assertFalse(okRejectionMessage.getFlag());
    assertTrue(okRejectionMessage.getMessage().contains("auth-required:"));

//    submit auth event
    TestSubscriber<OkMessage> authenticatedClient = new TestSubscriber<>();
    String subscriptionId = Factory.generateRandomHex64String();
    AfterimageMeshRelayService authdAimgRelay = new AfterimageMeshRelayService(afterimageRelayUri);
    CanonicalAuthenticationMessage authenticationMessage = createAuthenticationMessage(authorIdentity, subscriptionId, afterimageRelayUri);
    authdAimgRelay.send(
        authenticationMessage,
        authenticatedClient);

    TimeUnit.MILLISECONDS.sleep(250);

    OkMessage items = authenticatedClient.getItems().getFirst();

    assertTrue(items.getFlag());
    assertTrue(items.getMessage().contains(String.format("success: auth saved for pubkey [%s]", authorIdentity.getPublicKey())));

//  submit search relays list event to aImg w/ SC url, should:
//    1. get upvote event from SC
//    2. create REPUTATION event in aImg
    authdAimgRelay
        .send(
            new EventMessage(
                createSearchRelaysListEventMessage(superconductorRelayUrl)),
            authenticatedClient);

    List<OkMessage> scEventMessageResponse = authenticatedClient.getItems();
    assertEquals(true, scEventMessageResponse.getFirst().getFlag());
    TimeUnit.MILLISECONDS.sleep(5000);

    authdAimgRelay
        .send(
            new EventMessage(
                createSearchRelaysListEventMessage(superconductorRelayUrl_2)),
            authenticatedClient);

    List<OkMessage> sc2EventMessageResponse = authenticatedClient.getItems();
    assertEquals(true, sc2EventMessageResponse.getFirst().getFlag());
    TimeUnit.MILLISECONDS.sleep(5000);

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

    assertEquals("2", returnedReqGenericEvents_2.getFirst().getContent());
    assertEquals(returnedReqGenericEvents_2.getFirst().getPublicKey().toHexString(), afterimageInstanceIdentity.getPublicKey().toHexString());
    assertEquals(returnedReqGenericEvents_2.getFirst().getKind(), event.getKind());

//    more SC events
    BadgeAwardUpvoteEvent event_2 = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        upvoteBadgeDefinitionEvent);

    assertEquals(event_2.getPublicKey().toHexString(), authorIdentity.getPublicKey().toHexString());

//  submit upvote event to SC
    TestSubscriber<OkMessage> okMessageSubscriber_sc_1_2 = new TestSubscriber<>();
    new AfterimageMeshRelayService(superconductorRelayUrl).send(new EventMessage(event_2), okMessageSubscriber_sc_1_2);

    TestSubscriber<OkMessage> okMessageSubscriber_sc_1_3 = new TestSubscriber<>();
    new AfterimageMeshRelayService(superconductorRelayUrl).send(new EventMessage(event_2), okMessageSubscriber_sc_1_3);

    TimeUnit.MILLISECONDS.sleep(1000);

    List<OkMessage> items_4 = okMessageSubscriber_sc_1_2.getItems();
    assertEquals(true, items_4.getFirst().getFlag());

    TimeUnit.MILLISECONDS.sleep(100);

    List<OkMessage> items_5 = okMessageSubscriber_sc_1_3.getItems();
    assertEquals(true, items_5.getFirst().getFlag());

    TimeUnit.MILLISECONDS.sleep(100);

    log.debug("afterimage returned superconductor events:");
    List<BaseMessage> items_6 = afterImageEventsSubscriber_A.getItems();
    log.debug("  {}", items_6);

    TestSubscriber<BaseMessage> afterImageEventsSubscriber_B = new TestSubscriber<>();
    final AfterimageMeshRelayService afterimageRepRequestClient_2 = new AfterimageMeshRelayService(afterimageRelayUri);
    afterimageRepRequestClient_2.send(
        createAfterImageReqMessage(Factory.generateRandomHex64String(), upvotedUser.getPublicKey()),
        afterImageEventsSubscriber_B);

    List<BaseMessage> items_7 = afterImageEventsSubscriber_B.getItems();
    log.debug("  {}", items_7);

    List<EventIF> returnedReqGenericEvents_3 = getGenericEvents(items_7);
    assertEquals("3", returnedReqGenericEvents_3.getFirst().getContent());

    TestSubscriber<OkMessage> okMessageSubscriber_sc_2_2 = new TestSubscriber<>();
    new AfterimageMeshRelayService(superconductorRelayUrl_2).send(new EventMessage(event_2), okMessageSubscriber_sc_2_2);

    TestSubscriber<OkMessage> okMessageSubscriber_sc_2_3 = new TestSubscriber<>();
    new AfterimageMeshRelayService(superconductorRelayUrl_2).send(new EventMessage(event_2), okMessageSubscriber_sc_2_3);

    TestSubscriber<BaseMessage> afterImageEventsSubscriber_9 = new TestSubscriber<>();
    final AfterimageMeshRelayService afterimageRepRequestClient_3 = new AfterimageMeshRelayService(afterimageRelayUri);
    afterimageRepRequestClient_3.send(
        createAfterImageReqMessage(Factory.generateRandomHex64String(), upvotedUser.getPublicKey()),
        afterImageEventsSubscriber_9);

    List<BaseMessage> items_8 = afterImageEventsSubscriber_9.getItems();
    log.debug("  {}", items_8);

    List<EventIF> returnedReqGenericEvents_4 = getGenericEvents(items_8);
    assertEquals("3", returnedReqGenericEvents_4.getFirst().getContent());

    afterimageRepRequestClient_3.closeSocket();
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

  private CanonicalAuthenticationMessage createAuthenticationMessage(Identity identity, String subscriptionId, String uri) throws MalformedURLException, NoSuchAlgorithmException {
    return new CanonicalAuthenticationMessage(
        new CanonicalAuthenticationEvent(
            identity,
            "challenge_value",
            new Relay(uri)),
        subscriptionId);
  }

  private BaseEvent createSearchRelaysListEventMessage(String uri) {
    return new SearchRelaysListEvent(
        afterimageInstanceIdentity,
        "Kind.SEARCH_RELAYS_LIST",
        new RelayTag(
            new Relay(uri)));
  }
}
