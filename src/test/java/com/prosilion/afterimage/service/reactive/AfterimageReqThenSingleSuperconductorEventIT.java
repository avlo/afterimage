package com.prosilion.afterimage.service.reactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.config.TestcontainersConfig;
import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.EventServiceIF;
import com.prosilion.superconductor.base.service.event.service.GenericEventKind;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public class AfterimageReqThenSingleSuperconductorEventIT {
  public static final String afterimagePublicKey = "bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984";
  public static final String authorPublicKey = afterimagePublicKey;
  public static final String content = "matching kind, author, identity-tag filter test";

  private final AfterimageMeshRelayService superconductorRelayReactiveClient;
  private final AfterimageMeshRelayService afterimageMeshRelayService;
  private final EventServiceIF eventService;
  private final BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent;
  private final BadgeDefinitionReputationEvent badgeReputationDefinitionEvent;

  @Autowired
  public AfterimageReqThenSingleSuperconductorEventIT(
      @NonNull EventServiceIF eventService,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUri,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri,
      @NonNull BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent,
      @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) {
    this.superconductorRelayReactiveClient = new AfterimageMeshRelayService(superconductorRelayUri);
    this.afterimageMeshRelayService = new AfterimageMeshRelayService(afterimageRelayUri);
    this.badgeDefinitionUpvoteEvent = badgeDefinitionUpvoteEvent;
    this.badgeReputationDefinitionEvent = badgeDefinitionReputationEvent;
    this.eventService = eventService;
  }

  @Test
  void testAfterimageReqThenSuperconductorTwoEvents() throws IOException, NostrException {
    final Identity upvotedUser = Identity.generateRandomIdentity();
    System.out.println("upvotedUser.getPublicKey():  [" + upvotedUser.getPublicKey() + "]");
    final Identity authorIdentity = Identity.generateRandomIdentity();
    System.out.println("authorIdentity.getPublicKey():  [" + authorIdentity.getPublicKey() + "]");

//    // # --------------------- Aimg REQ -------------------
//    //   results should process at end of test once SC vote events have completed

    // # --------------------- SC EVENT 1 of 2-------------------
    //    begin event creation for submission to SC
    BadgeAwardUpvoteEvent badgeAwardUpvoteEvent_1 = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUser.getPublicKey(),
        badgeDefinitionUpvoteEvent);
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

    // # --------------------- SC REQ -------------------
    //    submit matching author & vote tag Req to superconductor

    TestSubscriber<BaseMessage> superConductorEventsSubscriber = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        createSuperconductorReqMessage(Factory.generateRandomHex64String()), superConductorEventsSubscriber);

    List<BaseMessage> returnedScMessages = superConductorEventsSubscriber.getItems();
    List<EventIF> returnedScEventIFs = getGenericEvents(returnedScMessages);

    assertTrue(returnedScEventIFs.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(badgeAwardUpvoteEvent_1.getContent())));
    assertTrue(returnedScEventIFs.stream().anyMatch(genericEvent -> genericEvent.getPublicKey().toHexString().equals(badgeAwardUpvoteEvent_1.getPublicKey().toHexString())));
    assertEquals(returnedScEventIFs.getFirst().getKind(), badgeAwardUpvoteEvent_1.getKind());
    assertTrue(returnedScEventIFs.stream().anyMatch(genericEvent -> genericEvent.getKind().equals(badgeAwardUpvoteEvent_1.getKind())));

    //    save SC result to Aimg
    //    should trigger Aimg afterImageEventsSubscriber


    List<EventMessage> eventMessages = returnedScEventIFs.stream().map(eventIF ->
        new EventMessage(createGenericEventRecord(eventIF))).toList();

    assertTrue(eventMessages.stream().allMatch(eventMessage -> {
      try {
        String expected = eventAsJson(eventMessage.getEvent());
        String actual = eventMessage.encode();
        return expected.equals(actual);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }));


    eventMessages.forEach(eventService::processIncomingEvent);

    TestSubscriber<BaseMessage> reputationRequestSubscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            upvotedUser.getPublicKey()),
        reputationRequestSubscriber);
    
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

    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF -> eventIF.getContent().equals("1")));

    superconductorRelayReactiveClient.closeSocket();
    afterimageMeshRelayService.closeSocket();
  }

  private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    List<EventMessage> eventMessages = returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast).toList();
    List<EventIF> list = eventMessages.stream()
        .map(EventMessage::getEvent)
        .toList();
    return list;
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
                badgeReputationDefinitionEvent.getIdentifierTag())));
  }

  private ReqMessage createSuperconductorReqMessage(String subscriberId) {
    return new ReqMessage(subscriberId,
        new Filters(
            new KindFilter(Kind.BADGE_AWARD_EVENT)));
  }

  private GenericEventRecord createGenericEventRecord(EventIF event) {
    GenericEventKind genericEventKind = new GenericEventKind(
        event.getId(),
        event.getPublicKey(),
        event.getCreatedAt(),
        event.getKind(),
        event.getTags(),
        event.getContent(),
        event.getSignature());
    return new GenericEventRecord(
        genericEventKind.getEventId(),
        genericEventKind.getPublicKey(),
        genericEventKind.getCreatedAt(),
        genericEventKind.getKind(),
        genericEventKind.getTags(),
        genericEventKind.getContent(),
        genericEventKind.getSignature());
  }

  private String eventAsJson(EventIF event) {
    AddressTag addressTag = Filterable.getTypeSpecificTags(AddressTag.class, event).getFirst();
    String pubkeyTagString = Filterable.getTypeSpecificTags(PubKeyTag.class, event).getFirst().getPublicKey().toHexString();
    String addressTagString = String.valueOf(addressTag.getKind().getValue()).concat(":").concat(addressTag.getPublicKey().toString()).concat(":").concat(addressTag.getIdentifierTag().getUuid());
    System.out.println(addressTagString);
    String s = "[\"EVENT\",{\"id\":\"" + event.getId() + "\",\"pubkey\":\"" + event.getPublicKey() + "\",\"created_at\":" + event.getCreatedAt() + ",\"kind\":" + event.getKind() + ",\"tags\":[" +
        "[\"a\",\"" + addressTagString + "\"]" +
        "," +
        "[\"p\",\"" + pubkeyTagString + "\"]" +
        "],\"content\":\"" + event.getContent() + "\",\"sig\":\"" + event.getSignature() + "\"}]";
    System.out.println(s);
    return s;
  }
}
