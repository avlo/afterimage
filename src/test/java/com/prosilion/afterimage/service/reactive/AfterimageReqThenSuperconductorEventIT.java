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
import java.io.IOException;
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
  void testAfterimageReqThenSuperconductorTwoEvents() throws IOException, NostrException {
    final Identity upvotedUser = Identity.generateRandomIdentity();
    final Identity authorIdentity = Identity.generateRandomIdentity();

//    // # --------------------- Aimg REQ -------------------
//    //   results should process at end of test once SC vote events have completed
    TestSubscriber<BaseMessage> reputationRequestSubscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            upvotedUser.getPublicKey()),
        reputationRequestSubscriber);

    // # --------------------- SC EVENT 1 of 2-------------------
    //    begin event creation for submission to SC
    BadgeAwardUpvoteEvent badgeAwardUpvoteEvent_1 = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        upvoteBadgeDefinitionEvent);
//    GenericEventKindTypeIF badgeAwardUpvoteEvent_1 =
//        new GenericDocumentKindTypeDto(
//            badgeAwardUpvoteEvent_1,
//            SuperconductorKindType.UNIT_UPVOTE)
//            .convertBaseEventToGenericEventKindTypeIF();

    //    submit subscriber's first Event to superconductor
    TestSubscriber<OkMessage> scEventSubmitter_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(badgeAwardUpvoteEvent_1), scEventSubmitter_1);
    assertEquals(true, scEventSubmitter_1
        .getItems()
        .getFirst()
        .getFlag());
    log.debug("received 1of2 OkMessage...");

    // # --------------------- SC EVENT 2 of 2-------------------
    BadgeAwardUpvoteEvent badgeAwardUpvoteEvent_2 = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        upvoteBadgeDefinitionEvent);
//    GenericEventKindTypeIF badgeAwardUpvoteEvent_2 =
//        new GenericDocumentKindTypeDto(
//            badgeAwardUpvoteEvent_2,
//            SuperconductorKindType.UNIT_UPVOTE)
//            .convertBaseEventToGenericEventKindTypeIF();

    TestSubscriber<OkMessage> scEventSubmitter_2 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(badgeAwardUpvoteEvent_2), scEventSubmitter_2);

    assertEquals(true, scEventSubmitter_2
        .getItems()
        .getFirst()
        .getFlag());
    log.debug("received 2of2 OkMessage...");

    // # --------------------- SC REQ -------------------
    //    submit matching author & vote tag Req to superconductor

    TestSubscriber<BaseMessage> superConductorEventsSubscriber = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(Factory.generateRandomHex64String()), superConductorEventsSubscriber);

    List<BaseMessage> returnedScMessages = superConductorEventsSubscriber.getItems();
    List<EventIF> returnedScEventIFs = getGenericEvents(returnedScMessages);

    assertTrue(returnedScEventIFs.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(badgeAwardUpvoteEvent_1.getContent())));
    assertTrue(returnedScEventIFs.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(badgeAwardUpvoteEvent_2.getContent())));
    assertTrue(returnedScEventIFs.stream().anyMatch(genericEvent -> genericEvent.getPublicKey().toHexString().equals(badgeAwardUpvoteEvent_1.getPublicKey().toHexString())));
    assertEquals(returnedScEventIFs.getFirst().getKind(), badgeAwardUpvoteEvent_1.getKind());
    assertTrue(returnedScEventIFs.stream().anyMatch(genericEvent -> genericEvent.getKind().equals(badgeAwardUpvoteEvent_1.getKind())));

    //    save SC result to Aimg
    //    should trigger Aimg afterImageEventsSubscriber
    returnedScEventIFs.forEach(eventIF ->
        eventService.processIncomingEvent(new EventMessage(createGenericEventRecord(eventIF))));


    // # --------------------- Aimg EVENTS returned -------------------
    List<BaseMessage> returnedAimgMessages = reputationRequestSubscriber.getItems();

    List<EventIF> returnedReputationEventIFs = getGenericEvents(returnedAimgMessages);
    log.debug("afterimage returned events:");
    returnedReputationEventIFs.forEach(eventIF -> log.debug(eventIF.getId()));
    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF -> eventIF.getTags()
        .stream()
        .filter(PubKeyTag.class::isInstance)
        .map(PubKeyTag.class::cast)
        .anyMatch(pubKeyTag -> pubKeyTag.getPublicKey().equals(upvotedUser.getPublicKey())))
    );

    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF -> eventIF.getContent().equals("2")));

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
