package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.config.SingleContainerTestConfig;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.AuthorFilter;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.filter.tag.ExternalIdentityTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.nostr.util.Util;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.subdivisions.client.reactive.NostrEventPublisher;
import com.prosilion.subdivisions.client.reactive.NostrSingleRelayRequestService;
import com.prosilion.subdivisions.client.reactive.NostrSingleRelayRequestServiceSubscriber;
import com.prosilion.superconductor.base.service.event.EventServiceIF;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(SingleContainerTestConfig.class)
public class AfterimageReqThenMultipleSuperconductorEventIT {

  /**
   * definitionsCreatorIdentity:   02d49b23e02985a760e8bc2f5ee86a3089569806f5f6a670fba3317568d14262
   * <p>
   * voteSubmitterIdentity:        611eda70943b4f67d1674068f5c86cedbdc3438bb41245b129a6311e4f308295
   * <p>
   * voteReceierIdentity:          985a5b9ea911bb8f9d9dca82c03f776d68fdc452b774295a874423a0fa5e8879
   */

  public static final String REPUTATION = "TEST_REPUTATION";
  public static final String AWARD_UNIT_UPVOTE = "TEST_UNIT_UPVOTE";
  public static final String FORMULA_UNIT_UPVOTE = "FORMULA_UNIT_UPVOTE";

  public static final String PLUS_ONE_FORMULA = "+1";

  private final IdentifierTag reputationIdentifierTag = new IdentifierTag(REPUTATION);
  private final IdentifierTag upvoteIdentifierTag = new IdentifierTag(AWARD_UNIT_UPVOTE);
  private final IdentifierTag formulaIdentifierTag = new IdentifierTag(FORMULA_UNIT_UPVOTE);

  private final Identity definitionsCreatorIdentity = Identity.generateRandomIdentity();

  private final EventServiceIF eventServiceIF;

  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventPlusOneFormula;
  private final BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent;

  private final Relay afterimageRelay;
  private final Relay superconductorRelay;
  Duration requestTimeoutDuration;

  @Autowired
  public AfterimageReqThenMultipleSuperconductorEventIT(
      @NonNull @Qualifier("eventService") EventServiceIF eventServiceIF,
      @NonNull RedisCacheServiceIF cacheServiceIF,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl,
      @NonNull Duration requestTimeoutDuration) throws ParseException, IOException {
    this.eventServiceIF = eventServiceIF;
    this.requestTimeoutDuration = requestTimeoutDuration;

    this.afterimageRelay = new Relay(afterimageRelayUrl);
    this.superconductorRelay = new Relay(superconductorRelayUrl);

    log.debug("definitionsCreatorIdentity: [{}]", definitionsCreatorIdentity.getPublicKey().toHexString());

////////////////////////////////
//    START SUPERCONDUCTOR section
//    create then submit SC awardUpvoteDefinitionEvent
    awardUpvoteDefinitionEvent = new BadgeDefinitionGenericEvent(definitionsCreatorIdentity, upvoteIdentifierTag, superconductorRelay); // 4444
    log.debug("sending awardUpvoteDefinitionEvent {} to superconductorRelayUrl", awardUpvoteDefinitionEvent);

    OkMessage upvoteDefinitionOkMessage = new NostrEventPublisher(superconductorRelay.getUrl()).send(
        new EventMessage(awardUpvoteDefinitionEvent));
    assertEquals(true, upvoteDefinitionOkMessage.getFlag());

//    validate SC awardUpvoteDefinitionEvent
    List<BaseMessage> scUpvoteDefinitionEventMessageItems = new NostrSingleRelayRequestService(superconductorRelay.getUrl()).send(
        createSuperconductorReqMessageBadgeDefinitionEvent(
            Factory.generateRandomHex64String(),
            definitionsCreatorIdentity.getPublicKey()));

    List<EventIF> returnedSuperconductorAwardUpvoteDefinitionEvents = getGenericEvents(scUpvoteDefinitionEventMessageItems);

    assertEquals(returnedSuperconductorAwardUpvoteDefinitionEvents.getFirst().getId(), awardUpvoteDefinitionEvent.getId());
    assertEquals(returnedSuperconductorAwardUpvoteDefinitionEvents.getFirst().getContent(), awardUpvoteDefinitionEvent.getContent());
    assertEquals(returnedSuperconductorAwardUpvoteDefinitionEvents.getFirst().getPublicKey().toHexString(), awardUpvoteDefinitionEvent.getPublicKey().toHexString());
    assertEquals(returnedSuperconductorAwardUpvoteDefinitionEvents.getFirst().getKind(), awardUpvoteDefinitionEvent.getKind());
//    END SUPERCONDUCTOR section
////////////////////////////////


////////////////////////////////
//    START AFTERIMAGE section
//    create then submit aImg FormulaEvent
    FormulaEvent plusOneFormulaEvent = new FormulaEvent(definitionsCreatorIdentity, formulaIdentifierTag, afterimageRelay, awardUpvoteDefinitionEvent, PLUS_ONE_FORMULA);
    log.debug("creator public key:\n\n  {}\n\n", definitionsCreatorIdentity.getPublicKey().toHexString());
    eventServiceIF.processIncomingEvent(
        new EventMessage(plusOneFormulaEvent));

//    validate aImg FormulaEvent item
    List<GenericEventRecord> returnedAimgFormulaEvents = cacheServiceIF.getEventsByKindAndAuthorPublicKeyAndIdentifierTag(
        Kind.ARBITRARY_CUSTOM_APP_DATA,
        definitionsCreatorIdentity.getPublicKey(),
        formulaIdentifierTag);

    assertEquals(returnedAimgFormulaEvents.getFirst().getId(), plusOneFormulaEvent.getId());
    assertEquals(returnedAimgFormulaEvents.getFirst().getContent(), plusOneFormulaEvent.getContent());
    assertEquals(returnedAimgFormulaEvents.getFirst().getPublicKey().toHexString(), plusOneFormulaEvent.getPublicKey().toHexString());
    assertEquals(returnedAimgFormulaEvents.getFirst().getKind(), plusOneFormulaEvent.getKind());

//    create then submit aImg BadgeDefinitionReputationEvent
    badgeDefinitionReputationEventPlusOneFormula = new BadgeDefinitionReputationEvent(definitionsCreatorIdentity, reputationIdentifierTag, afterimageRelay, BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG, plusOneFormulaEvent);
    eventServiceIF.processIncomingEvent(
        new EventMessage(badgeDefinitionReputationEventPlusOneFormula));

//    validate aImg BadgeDefinitionReputationEvent
    List<GenericEventRecord> returnedAImgBadgeDefinitionReputationEvents = cacheServiceIF.getEventsByKindAndAuthorPublicKeyAndIdentifierTag(
        Kind.BADGE_DEFINITION_EVENT,
        definitionsCreatorIdentity.getPublicKey(),
        reputationIdentifierTag);

    assertEquals(returnedAImgBadgeDefinitionReputationEvents.getFirst().getId(), badgeDefinitionReputationEventPlusOneFormula.getId());
    assertEquals(returnedAImgBadgeDefinitionReputationEvents.getFirst().getContent(), badgeDefinitionReputationEventPlusOneFormula.getContent());
    assertEquals(returnedAImgBadgeDefinitionReputationEvents.getFirst().getPublicKey().toHexString(), badgeDefinitionReputationEventPlusOneFormula.getPublicKey().toHexString());
    assertEquals(returnedAImgBadgeDefinitionReputationEvents.getFirst().getKind(), badgeDefinitionReputationEventPlusOneFormula.getKind());
  }

  @Test
  void testAfterimageReqThenSuperconductorTwoEvents() throws IOException, NostrException, InterruptedException {
    final Identity voteSubmitterIdentity = Identity.generateRandomIdentity();
    log.debug("voteSubmitterIdentity: [{}]", voteSubmitterIdentity.getPublicKey().toHexString());
    final Identity voteReceierIdentity = Identity.generateRandomIdentity();
    log.debug("voteReceierIdentity: [{}]", voteReceierIdentity.getPublicKey().toHexString());

//    // # --------------------- Aimg REQ -------------------
//    //   results should process at end of test once SC vote events have completed

    RequestSubscriber<BaseMessage> reputationRequestSubscriber = new RequestSubscriber<>(Duration.ofMinutes(3));
    NostrSingleRelayRequestServiceSubscriber afterimageMeshRelayService = new NostrSingleRelayRequestServiceSubscriber(afterimageRelay.getUrl());
    afterimageMeshRelayService.send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            voteReceierIdentity.getPublicKey(),
            definitionsCreatorIdentity.getPublicKey()),
        reputationRequestSubscriber);
    
    // # --------------------- SC EVENT 1 of 2-------------------
    //    begin event creation for submission to SC
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent_1 = createScUpvoteEvent(voteSubmitterIdentity, voteReceierIdentity, superconductorRelay);

//    GenericEventKindTypeIF badgeAwardUpvoteEvent_1 =
//        new GenericDocumentKindTypeDto(
//            badgeAwardUpvoteEvent_1,
//            SuperconductorKindType.UNIT_UPVOTE)
//            .convertBaseEventToGenericEventKindTypeIF();

    //    submit subscriber's first Event to superconductor
    OkMessage scEventSubmitter_1 = new NostrEventPublisher(superconductorRelay.getUrl()).send(new EventMessage(badgeAwardUpvoteEvent_1));
    assertEquals(true, scEventSubmitter_1.getFlag());
    log.debug("received 1of2 OkMessage...");

// # --------------------- SC REQ -------------------
//    submit matching author & vote tag Req to superconductor

    List<BaseMessage> returnedScMessages = new NostrSingleRelayRequestService(superconductorRelay.getUrl()).send(
        createSuperconductorReqMessageBadgeAwardEvent(
            Factory.generateRandomHex64String(),
            voteReceierIdentity.getPublicKey()));

    EventIF returnedScEventIF = getGenericEvents(returnedScMessages).getFirst();

//    assertTrue(returnedScEventIF.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(badgeAwardUpvoteEvent_1.getContent())));
//    assertTrue(returnedScEventIF.stream().anyMatch(genericEvent -> genericEvent.getPublicKey().toHexString().equals(badgeAwardUpvoteEvent_1.getPublicKey().toHexString())));
//    assertEquals(returnedScEventIF.getFirst().getKind(), badgeAwardUpvoteEvent_1.getKind());
//    assertTrue(returnedScEventIF.stream().anyMatch(genericEvent -> genericEvent.getKind().equals(badgeAwardUpvoteEvent_1.getKind())));

    assertEquals(returnedScEventIF.getId(), badgeAwardUpvoteEvent_1.getId());
    assertEquals(returnedScEventIF.getContent(), badgeAwardUpvoteEvent_1.getContent());
    assertEquals(returnedScEventIF.getPublicKey().toHexString(), badgeAwardUpvoteEvent_1.getPublicKey().toHexString());
    assertEquals(returnedScEventIF.getKind(), badgeAwardUpvoteEvent_1.getKind());

    //    save SC result to Aimg
    //    should trigger Aimg afterImageEventsSubscriber

    eventServiceIF.processIncomingEvent(
        new EventMessage(returnedScEventIF.asGenericEventRecord()));    
    
// # --------------------- SC EVENT 2 of 2-------------------
//    begin event creation for submission to SC
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent_2 = createScUpvoteEvent(voteSubmitterIdentity, voteReceierIdentity, superconductorRelay);

//    GenericEventKindTypeIF badgeAwardUpvoteEvent_1 =
//        new GenericDocumentKindTypeDto(
//            badgeAwardUpvoteEvent_1,
//            SuperconductorKindType.UNIT_UPVOTE)
//            .convertBaseEventToGenericEventKindTypeIF();

    //    submit subscriber's first Event to superconductor
    OkMessage scEventSubmitter_2 = new NostrEventPublisher(superconductorRelay.getUrl()).send(new EventMessage(badgeAwardUpvoteEvent_2));
    assertEquals(true, scEventSubmitter_2.getFlag());
    log.debug("received 2of2 OkMessage...");
    
    // # --------------------- SC REQ -------------------
    //    submit matching author & vote tag Req to superconductor

    List<BaseMessage> returnedScMessages2 = new NostrSingleRelayRequestService(superconductorRelay.getUrl()).send(
        createSuperconductorReqMessageBadgeAwardEvent(
            Factory.generateRandomHex64String(),
            voteReceierIdentity.getPublicKey()));

    EventIF returnedScEventIF2 = getGenericEvents(returnedScMessages2).getFirst();

//    assertTrue(returnedScEventIF2.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(badgeAwardUpvoteEvent_1.getContent())));
//    assertTrue(returnedScEventIF2.stream().anyMatch(genericEvent -> genericEvent.getPublicKey().toHexString().equals(badgeAwardUpvoteEvent_1.getPublicKey().toHexString())));
//    assertEquals(returnedScEventIF2.getFirst().getKind(), badgeAwardUpvoteEvent_1.getKind());
//    assertTrue(returnedScEventIF2.stream().anyMatch(genericEvent -> genericEvent.getKind().equals(badgeAwardUpvoteEvent_1.getKind())));

    assertEquals(returnedScEventIF2.getId(), badgeAwardUpvoteEvent_2.getId());
    assertEquals(returnedScEventIF2.getContent(), badgeAwardUpvoteEvent_2.getContent());
    assertEquals(returnedScEventIF2.getPublicKey().toHexString(), badgeAwardUpvoteEvent_2.getPublicKey().toHexString());
    assertEquals(returnedScEventIF2.getKind(), badgeAwardUpvoteEvent_2.getKind());

    //    save SC result to Aimg
    //    should trigger Aimg afterImageEventsSubscriber

    eventServiceIF.processIncomingEvent(
        new EventMessage(returnedScEventIF2.asGenericEventRecord()));

//    assertTrue(eventMessages.stream().allMatch(eventMessage -> {
//      try {
//        String expected = eventAsJson(eventMessage.getEvent());
//        String actual = eventMessage.encode();
//        return expected.equals(actual);
//      } catch (JsonProcessingException e) {
//        throw new RuntimeException(e);
//      }
//    }));
//
//    eventMessages.forEach(eventServiceIF::processIncomingEvent);

    // # --------------------- Aimg EVENTS returned -------------------
    TimeUnit.MILLISECONDS.sleep(1000);
    List<BaseMessage> returnedAimgMessages = reputationRequestSubscriber.getItems();
    List<EventIF> returnedReputationEventIFs = getGenericEvents(returnedAimgMessages);
    log.debug("afterimage returned events:");
    returnedReputationEventIFs.forEach(eventIF -> log.debug(eventIF.getId()));

    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF ->
        Filterable.getTypeSpecificTagsStream(PubKeyTag.class, eventIF)
            .map(PubKeyTag::getPublicKey)
            .anyMatch(publicKey -> publicKey.equals(voteReceierIdentity.getPublicKey()))));

    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF ->
        Filterable.getTypeSpecificTagsStream(AddressTag.class, eventIF)
            .map(AddressTag::getPublicKey)
            .anyMatch(definitionsCreatorIdentity.getPublicKey()::equals)));

    assertFalse(returnedReputationEventIFs.stream().anyMatch(eventIF ->
        Filterable.getTypeSpecificTagsStream(AddressTag.class, eventIF)
            .filter(addressTag -> addressTag.getKind().equals(Kind.BADGE_DEFINITION_EVENT))
            .filter(addressTag -> addressTag.getPublicKey().equals(definitionsCreatorIdentity.getPublicKey()))
            .filter(addressTag -> addressTag.getIdentifierTag().equals(reputationIdentifierTag))
            .toList().isEmpty()));

    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF ->
        Filterable.getTypeSpecificTagsStream(ExternalIdentityTag.class, eventIF)
            .anyMatch(BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG::equals)));

    log.debug("nnnnnnnnnnnnnnnnnn");
    log.debug("nnnnnnnnnnnnnnnnnn");

    log.debug("returnedAfterImageEvents.size() {}", returnedReputationEventIFs.size());
    log.debug("default sort order:");
    returnedReputationEventIFs.forEach(eventIF -> log.debug(eventIF.createPrettyPrintJson()));
    log.debug("------");
    returnedReputationEventIFs.forEach(eventIF -> log.debug("  {} : {}", eventIF.getContent(), eventIF.getCreatedAt()));

    log.debug("------");
    log.debug("sort order equals presorted? [{}]", returnedReputationEventIFs.equals(returnedReputationEventIFs.stream().sorted(Comparator.comparing(EventIF::getCreatedAt).reversed()).toList()));

    log.debug("------");
    log.debug("------");
    assertEquals(2, returnedReputationEventIFs.size());
//    assertEquals("1", returnedReputationEventIFs.getFirst().getContent());
    assertTrue(returnedReputationEventIFs.stream().map(EventIF::getContent).toList().contains("1"));
    assertTrue(returnedReputationEventIFs.stream().map(EventIF::getContent).toList().contains("2"));
  }

  private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    List<EventIF> list = returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
    return list;
  }

  private BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> createScUpvoteEvent(Identity voteSubmitterIdentity, Identity voteReceierIdentity, Relay relay) {
    return new BadgeAwardGenericEvent<>(
        voteSubmitterIdentity, // 1111
        voteReceierIdentity.getPublicKey(), // AAAAA
        relay,
        awardUpvoteDefinitionEvent);
  }

  private ReqMessage createAfterImageReqMessage(String subscriberId, PublicKey upvotedUserPublicKey, PublicKey badgeCreatorPublicKey) throws JsonProcessingException {
    ExternalIdentityTagFilter externalIdentityTagFilter = new ExternalIdentityTagFilter(BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG);
    ReqMessage reqMessageWithStuff = new ReqMessage(
        subscriberId,
        new Filters(
            new KindFilter(
                Kind.BADGE_AWARD_EVENT),
//            new IdentifierTagFilter(reputationIdentifierTag),
            new AddressTagFilter(
                new AddressTag(
                    Kind.BADGE_DEFINITION_EVENT,
                    badgeCreatorPublicKey,
                    reputationIdentifierTag)),
            new ReferencedPublicKeyFilter(
                new PubKeyTag(
                    upvotedUserPublicKey)),
            externalIdentityTagFilter));

    ReqMessage reqMessage = reqMessageWithStuff;
    log.debug(Util.prettyFormatJson(reqMessage.encode()));
    return reqMessage;
  }

  private ReqMessage createSuperconductorReqMessageBadgeAwardEvent(String subscriberId, PublicKey upvotedUserPublicKey) {
    return new ReqMessage(subscriberId,
        new Filters(
            new ReferencedPublicKeyFilter(new PubKeyTag(upvotedUserPublicKey)),
            new KindFilter(Kind.BADGE_AWARD_EVENT)));
  }

  private ReqMessage createSuperconductorReqMessageBadgeDefinitionEvent(String subscriberId, PublicKey badgeCreatorPublicKey) throws JsonProcessingException {
    ReqMessage reqMessage = new ReqMessage(subscriberId,
        new Filters(
            new AuthorFilter(badgeCreatorPublicKey),
            new KindFilter(Kind.BADGE_DEFINITION_EVENT)));
    String encodedJson = reqMessage.encode();
    String prettyJson = Util.prettyFormatJson(encodedJson);
    log.debug(encodedJson);
    log.debug(prettyJson);
    return reqMessage;
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
