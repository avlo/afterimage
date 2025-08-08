package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.service.DockerITComposeContainer;
import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.EventIF;
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
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import com.prosilion.superconductor.lib.redis.dto.GenericDocumentKindTypeDto;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
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
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
//@TestConfiguration(proxyBeanMethods = false)
//@ImportTestcontainers(DockerComposeContainer.class)
@ActiveProfiles("test")
public class SuperconductorEventThenAfterimageReqIT extends DockerITComposeContainer {

  private final EventServiceIF eventService;
  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
  private final PublicKey afterimageInstancePublicKey;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;
  private final String superconductorRelayUri;
  private final String afterimageRelayUri;

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
  void testAOrderSuperconductorEventThenAfterimageReq() throws IOException, NostrException, NoSuchAlgorithmException, InterruptedException {
    final Identity upvotedUser = Identity.generateRandomIdentity();
    final Identity authorIdentity = Identity.generateRandomIdentity();
    final AfterimageMeshRelayService superconductorRelayReactiveClient = new AfterimageMeshRelayService(superconductorRelayUri);
    final AfterimageMeshRelayService afterimageMeshRelayService = new AfterimageMeshRelayService(afterimageRelayUri);

    GenericEventKindTypeIF badgeAwardUpvoteEvent_1 =
        new GenericDocumentKindTypeDto(
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

    TimeUnit.SECONDS.sleep(1);

    List<OkMessage> items_1 = okMessageSubscriber_1.getItems();
    assertEquals(true, items_1.getFirst().getFlag());

    final String subscriberId_1 = Factory.generateRandomHex64String();
//    submit Req for above event to superconductor

    TestSubscriber<BaseMessage> superconductorEventsSubscriber_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(subscriberId_1),
        superconductorEventsSubscriber_1);

    TimeUnit.SECONDS.sleep(1);

    log.debug("retrieved afterimage events:");
    List<EventIF> returnedSuperconductorEvents =
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

    TimeUnit.SECONDS.sleep(1);

    log.debug("afterimage returned superconductor events:");
    List<BaseMessage> items_2 = afterImageEventsSubscriber_A.getItems();
    log.debug("  {}", items_2);

    List<EventIF> returnedReqGenericEvents_2 = getGenericEvents(items_2);

    assertEquals("1", returnedReqGenericEvents_2.getFirst().getContent());
    assertEquals(returnedReqGenericEvents_2.getFirst().getPublicKey().toHexString(), afterimageInstancePublicKey.toHexString());
    assertEquals(returnedReqGenericEvents_2.getFirst().getKind(), badgeAwardUpvoteEvent_1.getKind());
  }

  @Test
  void testBorderSuperconductorTwoEventsThenAfterimageReq() throws IOException, NostrException, NoSuchAlgorithmException, InterruptedException {
    final Identity upvotedUser = Identity.generateRandomIdentity();
    final Identity authorIdentity = Identity.generateRandomIdentity();
    final AfterimageMeshRelayService superconductorRelayReactiveClient = new AfterimageMeshRelayService(superconductorRelayUri);
    final AfterimageMeshRelayService afterimageMeshRelayService = new AfterimageMeshRelayService(afterimageRelayUri);

    BadgeAwardUpvoteEvent textNoteEvent_1 = new BadgeAwardUpvoteEvent(authorIdentity, upvotedUser.getPublicKey(), upvoteBadgeDefinitionEvent);
    GenericEventKindTypeIF genericEventKindIF = new GenericDocumentKindTypeDto(textNoteEvent_1, SuperconductorKindType.UPVOTE).convertBaseEventToGenericEventKindTypeIF();

    //    submit subscriber's first Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(genericEventKindIF), okMessageSubscriber_1);
    TimeUnit.MILLISECONDS.sleep(1500);
    
    List<OkMessage> items1 = okMessageSubscriber_1.getItems();
    TimeUnit.MILLISECONDS.sleep(1500);
    
    assertEquals(true, items1.getFirst().getFlag());
    log.debug("received 1of2 OkMessage...");

    BadgeAwardUpvoteEvent textNoteEvent_2 = new BadgeAwardUpvoteEvent(authorIdentity, upvotedUser.getPublicKey(), upvoteBadgeDefinitionEvent);
    GenericEventKindTypeIF genericEventKindIF2 = new GenericDocumentKindTypeDto(textNoteEvent_2, SuperconductorKindType.UPVOTE).convertBaseEventToGenericEventKindTypeIF();

//    okMessageSubscriber_1.dispose();
    TestSubscriber<OkMessage> okMessageSubscriber_2 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(genericEventKindIF2), okMessageSubscriber_2);
    TimeUnit.MILLISECONDS.sleep(1500);

    List<OkMessage> items = okMessageSubscriber_2.getItems();
    assertEquals(true, items.getFirst().getFlag());
    log.debug("received 2of2 OkMessage...");

// # --------------------- REQ -------------------    
//    submit votes Req to superconductor
    String subscriberId = Factory.generateRandomHex64String();

    TestSubscriber<BaseMessage> superConductorEventsSubscriber_W = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(subscriberId), superConductorEventsSubscriber_W);


    List<EventIF> returnedReqGenericEvents = getGenericEvents(
        superConductorEventsSubscriber_W.getItems());

    assertTrue(returnedReqGenericEvents.stream().map(EventIF::getId).anyMatch(textNoteEvent_1.getId()::equals));
    assertTrue(returnedReqGenericEvents.stream().map(EventIF::getPublicKey).map(PublicKey::toString).anyMatch(textNoteEvent_1.getPublicKey().toString()::equals));
    assertTrue(returnedReqGenericEvents.stream().map(EventIF::getKind).anyMatch(textNoteEvent_1.getKind()::equals));

//    save SC result to Aimg
    returnedReqGenericEvents.forEach(event -> eventService.processIncomingEvent(new EventMessage(event)));

    TimeUnit.SECONDS.sleep(1);

//    query Aimg for (as yet to be impl'd) reputation score event
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_V = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        createAfterImageReqMessage(subscriberId, upvotedUser.getPublicKey()), afterImageEventsSubscriber_V);

    TimeUnit.SECONDS.sleep(1);

    List<EventIF> returnedAfterImageEvents = getGenericEvents(
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

  synchronized public static List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
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

  synchronized private ReqMessage createSuperconductorReqMessage(String subscriberId) {
    return new ReqMessage(subscriberId,
        new Filters(
//            new ReferencedPublicKeyFilter(new PubKeyTag(upvotedUserPublicKey)),
            new KindFilter(Kind.BADGE_AWARD_EVENT)));
  }
}
