package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.relay.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
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
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.dto.GenericEventKindTypeDto;
import com.prosilion.superconductor.service.event.EventService;
import com.prosilion.superconductor.service.event.type.SuperconductorKindType;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestMethodOrder(MethodOrderer.class)
class SuperconductorEventThenAfterimageReqIT
//    extends CommonContainer 
{
  private final AfterimageMeshRelayService superconductorRelayReactiveClient;
  private final AfterimageMeshRelayService afterimageMeshRelayService;
  private final EventService eventService;
  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
  private final PublicKey afterimageInstancePublicKey;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;

  @Autowired
  SuperconductorEventThenAfterimageReqIT(
      @NonNull EventService eventService,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUri,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri,
      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent,
      @NonNull Identity afterimageInstanceIdentity) {
//    String serviceHost = superconductorContainer.getServiceHost("superconductor-afterimage", 5555);
//    log.debug("SuperconductorEventThenAfterimageReqIT host: {}", serviceHost);
    log.debug("superconductorRelayUri: {}", superconductorRelayUri);
    log.debug("afterimageRelayUri: {}", afterimageRelayUri);

    this.superconductorRelayReactiveClient = new AfterimageMeshRelayService(superconductorRelayUri);
    this.afterimageMeshRelayService = new AfterimageMeshRelayService(afterimageRelayUri);
    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.afterimageInstancePublicKey = afterimageInstanceIdentity.getPublicKey();
    this.eventService = eventService;
  }

  @Test
  @Order(1)
  void testSuperconductorEventThenAfterimageReq() throws IOException, NostrException, NoSuchAlgorithmException {
    final Identity upvotedUser = Identity.generateRandomIdentity();
    final Identity authorIdentity = Identity.generateRandomIdentity();

    GenericEventKindTypeIF badgeAwardUpvoteEvent_1 =
        new GenericEventKindTypeDto(
            new BadgeAwardUpvoteEvent(
                authorIdentity,
                upvotedUser.getPublicKey(),
                upvoteBadgeDefinitionEvent),
            SuperconductorKindType.UPVOTE)
            .convertBaseEventToGenericEventKindTypeIF();

    assertEquals(badgeAwardUpvoteEvent_1.getPublicKey().toHexString(), authorIdentity.getPublicKey().toHexString());

//    submit Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(badgeAwardUpvoteEvent_1), okMessageSubscriber_1);
    List<OkMessage> items_1 = okMessageSubscriber_1.getItems();
    assertEquals(true, items_1.getFirst().getFlag());

    final String subscriberId_1 = Factory.generateRandomHex64String();
//    submit Req for above event to superconductor

    TestSubscriber<BaseMessage> superconductorEventsSubscriber_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(subscriberId_1, upvotedUser.getPublicKey()),
        superconductorEventsSubscriber_1);

    log.debug("retrieved afterimage events:");
    List<GenericEventKindIF> returnedSuperconductorEvents =
        getGenericEvents(
            superconductorEventsSubscriber_1.getItems());

    assertEquals(returnedSuperconductorEvents.getFirst().getId(), badgeAwardUpvoteEvent_1.getId());
    assertEquals(returnedSuperconductorEvents.getFirst().getContent(), badgeAwardUpvoteEvent_1.getContent());
    assertEquals(returnedSuperconductorEvents.getFirst().getPublicKey().toHexString(), badgeAwardUpvoteEvent_1.getPublicKey().toHexString());
    assertEquals(returnedSuperconductorEvents.getFirst().getKind(), badgeAwardUpvoteEvent_1.getKind());

//    save SC result to Aimg
    returnedSuperconductorEvents.forEach(gev ->
        eventService.processIncomingEvent(new EventMessage(gev)));

//    query Aimg for above event
    final String subscriberId_2 = Factory.generateRandomHex64String();
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_A = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        createAfterImageReqMessage(subscriberId_2, upvotedUser.getPublicKey()),
        afterImageEventsSubscriber_A);

    log.debug("afterimage returned superconductor events:");
    List<BaseMessage> items_2 = afterImageEventsSubscriber_A.getItems();
    log.debug("  {}", items_2);

    List<GenericEventKindIF> returnedReqGenericEvents_2 = getGenericEvents(items_2);

    assertEquals("1", returnedReqGenericEvents_2.getFirst().getContent());
    assertEquals(returnedReqGenericEvents_2.getFirst().getPublicKey().toHexString(), afterimageInstancePublicKey.toHexString());
    assertEquals(returnedReqGenericEvents_2.getFirst().getKind(), badgeAwardUpvoteEvent_1.getKind());
  }

  @Test
  @Order(2)
  void testSuperconductorTwoEventsThenAfterimageReq() throws IOException, NostrException, NoSuchAlgorithmException, InterruptedException {
    final Identity upvotedUser = Identity.generateRandomIdentity();
    final Identity authorIdentity = Identity.generateRandomIdentity();

    BadgeAwardUpvoteEvent textNoteEvent_1 = new BadgeAwardUpvoteEvent(authorIdentity, upvotedUser.getPublicKey(), upvoteBadgeDefinitionEvent);
    GenericEventKindTypeIF genericEventKindIF = new GenericEventKindTypeDto(textNoteEvent_1, SuperconductorKindType.UPVOTE).convertBaseEventToGenericEventKindTypeIF();

    //    submit subscriber's first Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(genericEventKindIF), okMessageSubscriber_1);
    TimeUnit.SECONDS.sleep(1);
    List<OkMessage> items1 = okMessageSubscriber_1.getItems();
    TimeUnit.SECONDS.sleep(1);
    assertEquals(true, items1.getFirst().getFlag());
    log.debug("received 1of2 OkMessage...");

    BadgeAwardUpvoteEvent textNoteEvent_2 = new BadgeAwardUpvoteEvent(authorIdentity, upvotedUser.getPublicKey(), upvoteBadgeDefinitionEvent);
    GenericEventKindTypeIF genericEventKindIF2 = new GenericEventKindTypeDto(textNoteEvent_2, SuperconductorKindType.UPVOTE).convertBaseEventToGenericEventKindTypeIF();

//    okMessageSubscriber_1.dispose();
    TestSubscriber<OkMessage> okMessageSubscriber_2 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(genericEventKindIF2), okMessageSubscriber_2);
    TimeUnit.SECONDS.sleep(1);

    List<OkMessage> items = okMessageSubscriber_2.getItems();
    assertEquals(true, items.getFirst().getFlag());
    log.debug("received 2of2 OkMessage...");

// # --------------------- REQ -------------------    
//    submit matching author & vote tag Req to superconductor
    String subscriberId = Factory.generateRandomHex64String();

    TestSubscriber<BaseMessage> superConductorEventsSubscriber_W = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(subscriberId, upvotedUser.getPublicKey()), superConductorEventsSubscriber_W);


    List<GenericEventKindIF> returnedReqGenericEvents = getGenericEvents(
        superConductorEventsSubscriber_W.getItems());

    assertEquals(returnedReqGenericEvents.getFirst().getId(), textNoteEvent_1.getId());
    assertEquals(returnedReqGenericEvents.getFirst().getContent(), textNoteEvent_1.getContent());
    assertEquals(returnedReqGenericEvents.getFirst().getPublicKey().toHexString(), textNoteEvent_1.getPublicKey().toHexString());
    assertEquals(returnedReqGenericEvents.getFirst().getKind(), textNoteEvent_1.getKind());

//    save SC result to Aimg
    returnedReqGenericEvents.forEach(event -> eventService.processIncomingEvent(new EventMessage(event)));

    TimeUnit.SECONDS.sleep(1);

//    query Aimg for (as yet to be impl'd) reputation score event
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_V = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        createAfterImageReqMessage(subscriberId, upvotedUser.getPublicKey()), afterImageEventsSubscriber_V);

    TimeUnit.SECONDS.sleep(1);

    List<GenericEventKindIF> returnedAfterImageEvents = getGenericEvents(
        afterImageEventsSubscriber_V.getItems());

    TimeUnit.SECONDS.sleep(1);

    log.debug("000000000000000000");
    log.debug("000000000000000000");
    log.debug("{}", returnedAfterImageEvents.size());
    log.debug("------");
    returnedAfterImageEvents.forEach(a -> log.debug(a.getContent() + "\n----------\n"));
    log.debug("000000000000000000");
    log.debug("000000000000000000");

//    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getId().equals(textNoteEvent_1.getId())));
    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals("2")));
    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getPublicKey().toHexString().equals(afterimageInstancePublicKey.toHexString())));
    assertEquals(returnedAfterImageEvents.getFirst().getKind(), textNoteEvent_1.getKind());
    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getKind().equals(textNoteEvent_1.getKind())));
  }

  synchronized public static List<GenericEventKindIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
  }

  synchronized private ReqMessage createAfterImageReqMessage(String subscriberId, PublicKey upvotedUserPublicKey) {
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

  synchronized private ReqMessage createSuperconductorReqMessage(String subscriberId, PublicKey upvotedUserPublicKey) {
    return new ReqMessage(subscriberId,
        new Filters(
            new ReferencedPublicKeyFilter(new PubKeyTag(upvotedUserPublicKey)),
            new KindFilter(Kind.BADGE_AWARD_EVENT)));
  }
}
