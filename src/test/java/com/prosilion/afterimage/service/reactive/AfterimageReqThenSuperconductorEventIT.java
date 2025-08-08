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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.lang.NonNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class AfterimageReqThenSuperconductorEventIT extends DockerITComposeContainer {

  private final AfterimageMeshRelayService superconductorRelayReactiveClient;
  private final AfterimageMeshRelayService afterimageMeshRelayService;
  private final EventServiceIF eventService;
  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;

  @Autowired
  public AfterimageReqThenSuperconductorEventIT(
      @NonNull EventServiceIF eventService,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUri,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri,
      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    this.superconductorRelayReactiveClient = new AfterimageMeshRelayService(superconductorRelayUri);
    this.afterimageMeshRelayService = new AfterimageMeshRelayService(afterimageRelayUri);
    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.eventService = eventService;
  }

  @Test
  void testAfterimageReqThenSuperconductorTwoEvents() throws IOException, NostrException, NoSuchAlgorithmException, InterruptedException {
    final Identity upvotedUser = Identity.generateRandomIdentity();
    final Identity authorIdentity = Identity.generateRandomIdentity();

//    // # --------------------- Aimg EVENT -------------------
//    // query Aimg for (as yet to be impl'd) reputation score event
//    //   results should process at end of test once pre-req SC events have completed
    final String subscriberId_1 = Factory.generateRandomHex64String();
    TestSubscriber<BaseMessage> afterImageEventsSubscriber_A = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        createAfterImageReqMessage(subscriberId_1, upvotedUser.getPublicKey()), afterImageEventsSubscriber_A);

    // # --------------------- SC EVENT 1 of 2-------------------
    //    begin event creation for submission to SC
    GenericEventKindTypeIF badgeAwardUpvoteEvent_1 =
        new GenericDocumentKindTypeDto(
            new BadgeAwardUpvoteEvent(
                authorIdentity,
                upvotedUser.getPublicKey(),
                upvoteBadgeDefinitionEvent),
            SuperconductorKindType.UPVOTE)
            .convertBaseEventToGenericEventKindTypeIF();

    //    submit subscriber's first Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(badgeAwardUpvoteEvent_1), okMessageSubscriber_1);
    assertEquals(true, okMessageSubscriber_1
        .getItems()
        .getFirst()
        .getFlag());
    log.debug("received 1of2 OkMessage...");

    // # --------------------- SC EVENT 2 of 2-------------------
    GenericEventKindTypeIF badgeAwardUpvoteEvent_2 =
        new GenericDocumentKindTypeDto(
            new BadgeAwardUpvoteEvent(
                authorIdentity,
                upvotedUser.getPublicKey(),
                upvoteBadgeDefinitionEvent),
            SuperconductorKindType.UPVOTE)
            .convertBaseEventToGenericEventKindTypeIF();

    TestSubscriber<OkMessage> okMessageSubscriber_2 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(badgeAwardUpvoteEvent_2), okMessageSubscriber_2);

    assertEquals(true, okMessageSubscriber_2
        .getItems()
        .getFirst()
        .getFlag());
    log.debug("received 2of2 OkMessage...");

    // # --------------------- SC REQ -------------------
    //    submit matching author & vote tag Req to superconductor

    final String subscriberId_2 = Factory.generateRandomHex64String();
    TestSubscriber<BaseMessage> superConductorEventsSubscriber = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(subscriberId_2, upvotedUser.getPublicKey()), superConductorEventsSubscriber);


    List<BaseMessage> returnedSuperconductorBaseMessages = superConductorEventsSubscriber.getItems();
    List<EventIF> returnedSuperconductorEvents = getGenericEvents(returnedSuperconductorBaseMessages);

    assertTrue(returnedSuperconductorEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(badgeAwardUpvoteEvent_1.getContent())));
    assertTrue(returnedSuperconductorEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(badgeAwardUpvoteEvent_2.getContent())));
    assertTrue(returnedSuperconductorEvents.stream().anyMatch(genericEvent -> genericEvent.getPublicKey().toHexString().equals(badgeAwardUpvoteEvent_1.getPublicKey().toHexString())));
    assertEquals(returnedSuperconductorEvents.getFirst().getKind(), badgeAwardUpvoteEvent_1.getKind());
    assertTrue(returnedSuperconductorEvents.stream().anyMatch(genericEvent -> genericEvent.getKind().equals(badgeAwardUpvoteEvent_1.getKind())));

    //    save SC result to Aimg
    //    should trigger Aimg afterImageEventsSubscriber
    returnedSuperconductorEvents.forEach(gev ->
        eventService.processIncomingEvent(new EventMessage(gev)));


    // # --------------------- Aimg EVENTS returned -------------------
    List<BaseMessage> returnedAfterImageReqMessages = afterImageEventsSubscriber_A.getItems();

    List<EventIF> afterImageEvents = getGenericEvents(returnedAfterImageReqMessages);
    log.debug("afterimage returned events:");
    afterImageEvents.forEach(genericEvent -> log.debug(genericEvent.getId()));
    assertTrue(afterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getTags()
        .stream()
        .filter(PubKeyTag.class::isInstance)
        .map(PubKeyTag.class::cast)
        .anyMatch(pubKeyTag -> pubKeyTag.getPublicKey().equals(upvotedUser.getPublicKey())))
    );

    assertTrue(afterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals("1")));

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

  private ReqMessage createSuperconductorReqMessage(String subscriberId, PublicKey upvotedUserPublicKey) {
    return new ReqMessage(subscriberId,
        new Filters(
            new ReferencedPublicKeyFilter(new PubKeyTag(upvotedUserPublicKey)),
            new KindFilter(Kind.BADGE_AWARD_EVENT)));
  }
}
