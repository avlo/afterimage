package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.relay.AfterimageMeshRelayService;
import com.prosilion.afterimage.service.CommonContainer;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.GenericEventKindIF;
import com.prosilion.nostr.event.GenericEventKindTypeIF;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.dto.GenericEventKindTypeDto;
import com.prosilion.superconductor.service.event.EventService;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static com.prosilion.afterimage.service.reactive.SuperconductorEventThenAfterimageReqIT.getGenericEvents;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class AfterimageReqThenSuperconductorEventIT extends CommonContainer {
  private final AfterimageMeshRelayService superconductorRelayReactiveClient;
  private final AfterimageMeshRelayService afterimageMeshRelayService;
  private final EventService eventService;

  private final Identity authorIdentity = Identity.generateRandomIdentity();
  private final static String subscriberId = Factory.generateRandomHex64String();

  @Autowired
  AfterimageReqThenSuperconductorEventIT(
      @NonNull EventService eventService,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUri,
      @NonNull AfterimageMeshRelayService afterimageMeshRelayService
  ) {
//    String serviceHost = superconductorContainer.getServiceHost("superconductor-afterimage", 5555);
//    log.debug("SuperconductorEventThenAfterimageReqIT host: {}", serviceHost);
    log.debug("SuperconductorEventThenAfterimageReqIT hash: {}", superconductorRelayUri.hashCode());
    this.superconductorRelayReactiveClient = new AfterimageMeshRelayService(superconductorRelayUri);
    this.afterimageMeshRelayService = afterimageMeshRelayService;
    this.eventService = eventService;
  }

  @Test
  void testAfterimageReqThenSuperconductorTwoEvents() throws IOException, NostrException, NoSuchAlgorithmException {
    final Identity upvotedUser = Identity.generateRandomIdentity();
//    // # --------------------- Aimg EVENT -------------------
//    // query Aimg for (as yet to be impl'd) reputation score event
//    //   results should process at end of test once pre-req SC events have completed
    TestSubscriber<BaseMessage> afterImageEventsSubscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        createAfterImageReqMessage(subscriberId, upvotedUser.getPublicKey()), afterImageEventsSubscriber);

    // # --------------------- SC EVENT 1 of 2-------------------
    //    begin event creation for submission to SC
    GenericEventKindTypeIF badgeAwardUpvoteEvent_1 =
        new GenericEventKindTypeDto(
            new BadgeAwardUpvoteEvent(
                authorIdentity,
                upvotedUser.getPublicKey()),
            AfterimageKindType.UPVOTE)
            .convertBaseEventToGenericEventKindTypeIF();

    //    submit subscriber's first Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(badgeAwardUpvoteEvent_1), okMessageSubscriber_1);
    assertEquals(true, okMessageSubscriber_1
        .getItems()
        .getFirst()
        .getFlag());
    log.debug("received 1of2 OkMessage...");

    GenericEventKindTypeIF badgeAwardUpvoteEvent_2 =
        new GenericEventKindTypeDto(
            new BadgeAwardUpvoteEvent(
                authorIdentity,
                upvotedUser.getPublicKey()),
            AfterimageKindType.UPVOTE)
            .convertBaseEventToGenericEventKindTypeIF();

    // # --------------------- SC EVENT 2 of 2-------------------

    TestSubscriber<OkMessage> okMessageSubscriber = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(badgeAwardUpvoteEvent_2), okMessageSubscriber);

    assertEquals(true, okMessageSubscriber_1
        .getItems()
        .getFirst()
        .getFlag());
    log.debug("received 2of2 OkMessage...");

    // # --------------------- SC REQ -------------------
    //    submit matching author & vote tag Req to superconductor

    TestSubscriber<BaseMessage> superConductorEventsSubscriber = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(subscriberId, upvotedUser.getPublicKey()), superConductorEventsSubscriber);

    List<BaseMessage> returnedSuperconductorBaseMessages = superConductorEventsSubscriber.getItems();
    List<GenericEventKindIF> returnedSuperconductorEvents = getGenericEvents(returnedSuperconductorBaseMessages);

    assertTrue(returnedSuperconductorEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(badgeAwardUpvoteEvent_1.getContent())));
    assertTrue(returnedSuperconductorEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(badgeAwardUpvoteEvent_2.getContent())));
    assertTrue(returnedSuperconductorEvents.stream().anyMatch(genericEvent -> genericEvent.getPublicKey().toHexString().equals(badgeAwardUpvoteEvent_1.getPublicKey().toHexString())));
    assertEquals(returnedSuperconductorEvents.getFirst().getKind(), badgeAwardUpvoteEvent_1.getKind());
    assertTrue(returnedSuperconductorEvents.stream().anyMatch(genericEvent -> genericEvent.getKind().equals(badgeAwardUpvoteEvent_1.getKind())));

    //    save SC result to Aimg
    //    should trigger Aimg afterImageEventsSubscriber
    returnedSuperconductorEvents.forEach(event ->
        eventService.processIncomingEvent(new EventMessage(event)));

    // # --------------------- Aimg EVENTS returned -------------------
    List<BaseMessage> returnedAfterImageReqMessages = afterImageEventsSubscriber.getItems();
    List<GenericEventKindIF> afterImageEvents = getGenericEvents(returnedAfterImageReqMessages);
    log.debug("afterimage returned events:");
    afterImageEvents.forEach(genericEvent -> log.debug(genericEvent.getId()));
    assertTrue(afterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(badgeAwardUpvoteEvent_1.getContent())));
    assertTrue(afterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(badgeAwardUpvoteEvent_2.getContent())));
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
                new IdentifierTag(
                    AfterimageKindType.REPUTATION.getName()))));
  }

  private ReqMessage createSuperconductorReqMessage(String subscriberId, PublicKey upvotedUserPublicKey) {
    return new ReqMessage(subscriberId,
        new Filters(
            new ReferencedPublicKeyFilter(new PubKeyTag(upvotedUserPublicKey)),
            new KindFilter(Kind.BADGE_AWARD_EVENT)));
  }
}
