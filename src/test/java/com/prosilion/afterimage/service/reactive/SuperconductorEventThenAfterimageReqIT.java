package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.relay.AfterimageMeshRelayService;
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
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class SuperconductorEventThenAfterimageReqIT
//    extends CommonContainer 
{
  private final AfterimageMeshRelayService superconductorRelayReactiveClient;
  private final AfterimageMeshRelayService afterimageMeshRelayService;
  private final EventService eventService;
  private final Identity afterimageInstanceIdentity;

  @Autowired
  SuperconductorEventThenAfterimageReqIT(
      @NonNull EventService eventService,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUri,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri,
      @NonNull Identity afterimageInstanceIdentity) {
//    String serviceHost = superconductorContainer.getServiceHost("superconductor-afterimage", 5555);
//    log.debug("SuperconductorEventThenAfterimageReqIT host: {}", serviceHost);
    log.debug("superconductorRelayUri: {}", superconductorRelayUri);
    log.debug("afterimageRelayUri: {}", afterimageRelayUri);

    this.superconductorRelayReactiveClient = new AfterimageMeshRelayService(superconductorRelayUri);
    this.afterimageMeshRelayService = new AfterimageMeshRelayService(afterimageRelayUri);
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.eventService = eventService;
  }

  @Test
  void testSuperconductorEventThenAfterimageReq() throws IOException, NostrException, NoSuchAlgorithmException {
    final Identity upvotedUser = Identity.generateRandomIdentity();
    final Identity authorIdentity = afterimageInstanceIdentity;

    GenericEventKindTypeIF badgeAwardUpvoteEvent =
        new GenericEventKindTypeDto(
            new BadgeAwardUpvoteEvent(
                authorIdentity,
                upvotedUser.getPublicKey()),
            AfterimageKindType.UPVOTE)
            .convertBaseEventToGenericEventKindTypeIF();

    assertEquals(badgeAwardUpvoteEvent.getPublicKey().toHexString(), authorIdentity.getPublicKey().toHexString());

//    submit Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(badgeAwardUpvoteEvent), okMessageSubscriber);
    List<OkMessage> items2 = okMessageSubscriber.getItems();
    assertEquals(true, items2.getFirst().getFlag());

    final String subscriberId = Factory.generateRandomHex64String();
//    submit Req for above event to superconductor

    TestSubscriber<BaseMessage> superconductorEventsSubscriber_X = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(subscriberId, upvotedUser.getPublicKey()),
        superconductorEventsSubscriber_X);

    log.debug("retrieved afterimage events:");
    List<BaseMessage> items = superconductorEventsSubscriber_X.getItems();
    List<GenericEventKindIF> returnedReqGenericEvents = getGenericEvents(items);

    assertEquals(returnedReqGenericEvents.getFirst().getId(), badgeAwardUpvoteEvent.getId());
    assertEquals(returnedReqGenericEvents.getFirst().getContent(), badgeAwardUpvoteEvent.getContent());
    assertEquals(returnedReqGenericEvents.getFirst().getPublicKey().toHexString(), badgeAwardUpvoteEvent.getPublicKey().toHexString());
    assertEquals(returnedReqGenericEvents.getFirst().getKind(), badgeAwardUpvoteEvent.getKind());

//    save SC result to Aimg
    eventService.processIncomingEvent(new EventMessage(returnedReqGenericEvents.getFirst()));

//    query Aimg for above event
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_Y = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        createAfterImageReqMessage(subscriberId, upvotedUser.getPublicKey()),
        afterImageEventsSubscriber_Y);

    log.debug("afterimage returned superconductor events:");
    List<BaseMessage> items1 = afterImageEventsSubscriber_Y.getItems();
    log.debug("  {}", items1);

    List<GenericEventKindIF> returnedReqGenericEvents1 = getGenericEvents(items1);

    assertEquals(returnedReqGenericEvents1.getFirst().getContent(), badgeAwardUpvoteEvent.getContent());
    assertEquals(returnedReqGenericEvents1.getFirst().getPublicKey().toHexString(), badgeAwardUpvoteEvent.getPublicKey().toHexString());
    assertEquals(returnedReqGenericEvents1.getFirst().getKind(), badgeAwardUpvoteEvent.getKind());
  }

  @Test
  void testSuperconductorTwoEventsThenAfterimageReq() throws IOException, NostrException, NoSuchAlgorithmException {
    final Identity upvotedUser = Identity.generateRandomIdentity();
    final Identity authorIdentity = afterimageInstanceIdentity;

    BadgeAwardUpvoteEvent textNoteEvent_1 = new BadgeAwardUpvoteEvent(authorIdentity, upvotedUser.getPublicKey());
    GenericEventKindTypeIF genericEventKindIF = new GenericEventKindTypeDto(textNoteEvent_1, AfterimageKindType.UPVOTE).convertBaseEventToGenericEventKindTypeIF();

    //    submit subscriber's first Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(genericEventKindIF), okMessageSubscriber_1);
    List<OkMessage> items1 = okMessageSubscriber_1.getItems();
    assertEquals(true, items1.getFirst().getFlag());
    log.debug("received 1of2 OkMessage...");

    BadgeAwardUpvoteEvent textNoteEvent_2 = new BadgeAwardUpvoteEvent(authorIdentity, upvotedUser.getPublicKey());
    GenericEventKindTypeIF genericEventKindIF2 = new GenericEventKindTypeDto(textNoteEvent_2, AfterimageKindType.UPVOTE).convertBaseEventToGenericEventKindTypeIF();

//    submit subscriber's second Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber_2 = new TestSubscriber<>();

//    okMessageSubscriber_1.dispose();
    superconductorRelayReactiveClient.send(new EventMessage(genericEventKindIF2), okMessageSubscriber_2);

    List<OkMessage> items = okMessageSubscriber_2.getItems();
    assertEquals(true, items.getFirst().getFlag());
    log.debug("received 2of2 OkMessage...");

// # --------------------- REQ -------------------    
//    submit matching author & vote tag Req to superconductor
    String subscriberId = Factory.generateRandomHex64String();

    TestSubscriber<BaseMessage> superConductorEventsSubscriber_W = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(subscriberId, upvotedUser.getPublicKey()), superConductorEventsSubscriber_W);

    List<BaseMessage> superCondutorEvents = superConductorEventsSubscriber_W.getItems();
    List<GenericEventKindIF> returnedReqGenericEvents = getGenericEvents(superCondutorEvents);

    assertEquals(returnedReqGenericEvents.getFirst().getId(), textNoteEvent_1.getId());
    assertEquals(returnedReqGenericEvents.getFirst().getContent(), textNoteEvent_1.getContent());
    assertEquals(returnedReqGenericEvents.getFirst().getPublicKey().toHexString(), textNoteEvent_1.getPublicKey().toHexString());
    assertEquals(returnedReqGenericEvents.getFirst().getKind(), textNoteEvent_1.getKind());

//    save SC result to Aimg
    superCondutorEvents.forEach(event -> eventService.processIncomingEvent(new EventMessage(returnedReqGenericEvents.getFirst())));

//    query Aimg for (as yet to be impl'd) reputation score event
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_V = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        createAfterImageReqMessage(subscriberId, upvotedUser.getPublicKey()), afterImageEventsSubscriber_V);

    List<BaseMessage> afterImageEvents = afterImageEventsSubscriber_V.getItems();
    List<GenericEventKindIF> returnedAfterImageEvents = getGenericEvents(afterImageEvents);

//    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getId().equals(textNoteEvent_1.getId())));
    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(textNoteEvent_1.getContent())));
    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getPublicKey().toHexString().equals(textNoteEvent_1.getPublicKey().toHexString())));
    assertEquals(returnedAfterImageEvents.getFirst().getKind(), textNoteEvent_1.getKind());
    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getKind().equals(textNoteEvent_1.getKind())));
  }

  public static List<GenericEventKindIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
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
