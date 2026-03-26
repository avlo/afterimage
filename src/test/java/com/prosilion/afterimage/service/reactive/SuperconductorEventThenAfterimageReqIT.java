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
import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
import com.prosilion.superconductor.base.service.event.EventServiceIF;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
public class SuperconductorEventThenAfterimageReqIT {

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
  private final Identity definitionsCreatorIdentity =
//      Identity.generateRandomIdentity();
      Identity.create("bbb4585483196998204846989544737603523651520600328805626488477202");

  private final EventServiceIF eventServiceIF;

  private final BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent;

  private final String superconductorRelayUrl;
  private final String afterimageRelayUrl;
  private final Relay superconductorRelay;

  @Autowired
  public SuperconductorEventThenAfterimageReqIT(
      @NonNull @Qualifier("eventService") EventServiceIF eventServiceIF,
      @NonNull RedisCacheServiceIF cacheServiceIF,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) throws ParseException, IOException, InterruptedException {
    this.superconductorRelayUrl = superconductorRelayUrl;
    this.afterimageRelayUrl = afterimageRelayUrl;
    this.eventServiceIF = eventServiceIF;

    Relay afterimageRelay = new Relay(afterimageRelayUrl);
    this.superconductorRelay = new Relay(superconductorRelayUrl);

    log.debug("definitionsCreatorIdentity: [{}]", definitionsCreatorIdentity.getPublicKey().toHexString());

////////////////////////////////
//    START SUPERCONDUCTOR section
//    create then submit SC awardUpvoteDefinitionEvent
    IdentifierTag upvoteIdentifierTag = new IdentifierTag(AWARD_UNIT_UPVOTE);
    awardUpvoteDefinitionEvent = new BadgeDefinitionGenericEvent(definitionsCreatorIdentity, upvoteIdentifierTag, superconductorRelay); // 4444
    log.debug("sending awardUpvoteDefinitionEvent {} to superconductorRelayUrl", awardUpvoteDefinitionEvent);
    NostrEventPublisher superconductorRelayClient = new NostrEventPublisher(superconductorRelayUrl);
    RequestSubscriber<OkMessage> awardUpvoteDefinitionEventSubscriber = new RequestSubscriber<>(Duration.ofMillis(10000));
    superconductorRelayClient.send(
        new EventMessage(awardUpvoteDefinitionEvent), awardUpvoteDefinitionEventSubscriber);
    assertEquals(true, awardUpvoteDefinitionEventSubscriber.getItems().getFirst().getFlag());

//    validate SC awardUpvoteDefinitionEvent item
    List<BaseMessage> scUpvoteDefinitionEventMessageItems = new NostrSingleRequestService().send(
        createSuperconductorReqMessageBadgeDefinitionEvent(
            Factory.generateRandomHex64String(),
            definitionsCreatorIdentity.getPublicKey()),
        superconductorRelayUrl);

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
    IdentifierTag formulaIdentifierTag = new IdentifierTag(FORMULA_UNIT_UPVOTE);
    FormulaEvent plusOneFormulaEvent = new FormulaEvent(definitionsCreatorIdentity, formulaIdentifierTag, afterimageRelay, awardUpvoteDefinitionEvent, PLUS_ONE_FORMULA); // 55555 aImg
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

//    create then submit aImg BadgeDefinitionReputationEvent item
    BadgeDefinitionReputationEvent badgeDefinitionReputationEventPlusOneFormula = new BadgeDefinitionReputationEvent(definitionsCreatorIdentity, reputationIdentifierTag, afterimageRelay, BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG, plusOneFormulaEvent);
    eventServiceIF.processIncomingEvent(
        new EventMessage(badgeDefinitionReputationEventPlusOneFormula));

//    validate aImg BadgeDefinitionReputationEvent item
    List<GenericEventRecord> returnedAImgBadgeDefinitionReputationEvents = cacheServiceIF.getEventsByKindAndAuthorPublicKeyAndIdentifierTag(
        Kind.BADGE_DEFINITION_EVENT,
        definitionsCreatorIdentity.getPublicKey(),
        reputationIdentifierTag);

    assertEquals(returnedAImgBadgeDefinitionReputationEvents.getFirst().getId(), badgeDefinitionReputationEventPlusOneFormula.getId());
    assertEquals(returnedAImgBadgeDefinitionReputationEvents.getFirst().getContent(), badgeDefinitionReputationEventPlusOneFormula.getContent());
    assertEquals(returnedAImgBadgeDefinitionReputationEvents.getFirst().getPublicKey().toHexString(), badgeDefinitionReputationEventPlusOneFormula.getPublicKey().toHexString());
    assertEquals(returnedAImgBadgeDefinitionReputationEvents.getFirst().getKind(), badgeDefinitionReputationEventPlusOneFormula.getKind());
//    END AFTERIMAGE section
////////////////////////////////
    TimeUnit.MILLISECONDS.sleep(1000);
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException, InterruptedException {
    final Identity voteSubmitterIdentity = Identity.generateRandomIdentity();
    //Identity.create("aaa4585483196998204846989544737603523651520600328805626488477202");
    log.debug("voteSubmitterIdentity: [{}]", voteSubmitterIdentity.getPublicKey().toHexString());
    final Identity voteReceierIdentity = Identity.generateRandomIdentity();
//        Identity.create("ccc4585483196998204846989544737603523651520600328805626488477202");
    log.debug("voteReceierIdentity: [{}]", voteReceierIdentity.getPublicKey().toHexString());

    List<BaseMessage> initialItems = new NostrSingleRequestService().send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            voteReceierIdentity.getPublicKey(),
            definitionsCreatorIdentity.getPublicKey()),
        afterimageRelayUrl);

    // TimeUnit.MILLISECONDS.sleep(1000);
    //  test initial aImg events state, should have zero reputation events for voteReceierIdentity

    log.debug("AAAAAAAAAAAAAA");
    log.debug("AAAAAAAAAAAAAA");
    log.debug("afterimage initial events:");
    log.debug("BBBBBBBBBBBBBBB");
    log.debug("BBBBBBBBBBBBBBB");
    log.debug("  {}", initialItems);

    List<EventIF> initialEvents = getGenericEvents(initialItems);
    log.debug("CCCCCCCCCCCCCCC");
    log.debug("CCCCCCCCCCCCCCC");
    assertEquals(0, initialEvents.size());
    log.debug("ccccccccccccccc");
    log.debug("ccccccccccccccc");
//    afterimageSubscriberCheckClient.closeSocket();

//  begin SC votes submissions  
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> scBadgeAwardUpvoteEvent_1 = createScUpvoteEvent(voteSubmitterIdentity, voteReceierIdentity);

//  submit first Event to superconductor
    OkMessage items_1 = new NostrEventPublisher(superconductorRelayUrl).send(
        new EventMessage(scBadgeAwardUpvoteEvent_1), Duration.ofMillis(10000));

    // TimeUnit.MILLISECONDS.sleep(2500);

    log.debug("EEEEEEEEEEEEEEE");
    log.debug("EEEEEEEEEEEEEEE");
    assertEquals(true, items_1.getFlag());

//    validate by submit Req for above badgeAwardUpvoteEvent to superconductor
    List<BaseMessage> superconductorEventsSubscriber_1a_Items = new NostrSingleRequestService().send(
        createSuperconductorReqMessageBadgeAwardEvent(Factory.generateRandomHex64String(), voteReceierIdentity.getPublicKey()),
        superconductorRelayUrl);

    // TimeUnit.MILLISECONDS.sleep(2500);

    log.debug("ffffffffffffff");
    log.debug("ffffffffffffff");
    log.debug("retrieved afterimage events:");
    EventIF returnedScBadgeAwardUpvoteEvent_1a = getGenericEvents(superconductorEventsSubscriber_1a_Items).getFirst();

    assertEquals(returnedScBadgeAwardUpvoteEvent_1a.getId(), scBadgeAwardUpvoteEvent_1.getId());
    assertEquals(returnedScBadgeAwardUpvoteEvent_1a.getContent(), scBadgeAwardUpvoteEvent_1.getContent());
    assertEquals(returnedScBadgeAwardUpvoteEvent_1a.getPublicKey().toHexString(), scBadgeAwardUpvoteEvent_1.getPublicKey().toHexString());
    assertEquals(returnedScBadgeAwardUpvoteEvent_1a.getKind(), scBadgeAwardUpvoteEvent_1.getKind());

    // TimeUnit.MILLISECONDS.sleep(1000);

////    simulate Aimg FollowSets handling, inserting 1st SC upvote into aImg
    eventServiceIF.processIncomingEvent(
        new EventMessage(returnedScBadgeAwardUpvoteEvent_1a.asGenericEventRecord()));
//    cacheServiceIF.save(returnedScBadgeAwardUpvoteEvent_1a);

//    query Aimg for above badgeAwardUpvoteEvent
    log.debug("gggggggggggggg");
    log.debug("gggggggggggggg");
    log.debug("query Aimg for above badgeAwardUpvoteEvent:");
    List<BaseMessage> aimgSubscriberItems_A = new NostrSingleRequestService().send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            voteReceierIdentity.getPublicKey(),
            definitionsCreatorIdentity.getPublicKey()),
        afterimageRelayUrl);

    // TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("hhhhhhhhhhhhhh");
    log.debug("hhhhhhhhhhhhhh");
    log.debug("afterimage returned events:");
    List<EventIF> returnedAimgReqGenericEvents_A = getGenericEvents(aimgSubscriberItems_A);

    assertEquals("1", returnedAimgReqGenericEvents_A.getFirst().getContent());
//    assertTrue(Filterable.getTypeSpecificTags(PubKeyTag.class, returnedAimgReqGenericEvents_A.getFirst()).stream().map(PubKeyTag::getPublicKey).map(PublicKey::toHexString).anyMatch(badgeDefinitionUpvoteCreatorPubkey_3333.getPublicKey().toString()::equals));
    assertEquals(returnedAimgReqGenericEvents_A.getFirst().getKind(), scBadgeAwardUpvoteEvent_1.getKind());

    //    create & submit subscriber's second Event to superconductor
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> scBadgeAwardUpvoteEvent_2 = createScUpvoteEvent(voteSubmitterIdentity, voteReceierIdentity);

    OkMessage scOkMessageSubscriber_2 = new NostrEventPublisher(superconductorRelayUrl).send(
        new EventMessage(scBadgeAwardUpvoteEvent_2), Duration.ofMillis(10000));
    // TimeUnit.MILLISECONDS.sleep(1000);

    assertEquals(true, scOkMessageSubscriber_2.getFlag());
    log.debug("received 2of2 OkMessage...");

    List<BaseMessage> superconductorEventsSubscriber_2a_Items = new NostrSingleRequestService().send(
        createSuperconductorReqMessageBadgeAwardEvent(Factory.generateRandomHex64String(), voteReceierIdentity.getPublicKey()),
        superconductorRelayUrl);

    // TimeUnit.MILLISECONDS.sleep(2500);

    log.debug("iiiiiiiiiiiiii");
    log.debug("iiiiiiiiiiiiii");
    log.debug("retrieved afterimage events:");
    List<EventIF> returnedScBadgeAwardUpvoteEvent_2a = getGenericEvents(superconductorEventsSubscriber_2a_Items);

    assertEquals(returnedScBadgeAwardUpvoteEvent_2a.getFirst().getId(), scBadgeAwardUpvoteEvent_2.getId());
    assertEquals(returnedScBadgeAwardUpvoteEvent_2a.getFirst().getContent(), scBadgeAwardUpvoteEvent_2.getContent());
    assertEquals(returnedScBadgeAwardUpvoteEvent_2a.getFirst().getPublicKey().toHexString(), scBadgeAwardUpvoteEvent_2.getPublicKey().toHexString());
    assertEquals(returnedScBadgeAwardUpvoteEvent_2a.getFirst().getKind(), scBadgeAwardUpvoteEvent_2.getKind());

//    simulate Aimg FollowSets handling, inserting SC upvote into aImg
    EventIF eventIF2 = returnedScBadgeAwardUpvoteEvent_2a.stream().max(Comparator.comparing(EventIF::getCreatedAt)).orElseThrow();
    eventServiceIF.processIncomingEvent(
        new EventMessage(eventIF2.asGenericEventRecord()));
//    cacheServiceIF.save(returnedScBadgeAwardUpvoteEvent_2a.getFirst());

//    create & submit subscriber's 3rd Event to superconductor
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> scBadgeAwardUpvoteEvent_3 = createScUpvoteEvent(voteSubmitterIdentity, voteReceierIdentity);

    OkMessage scOkMessageSubscriber_3 = new NostrEventPublisher(superconductorRelayUrl).send(
        new EventMessage(scBadgeAwardUpvoteEvent_3), Duration.ofMillis(10000));
    // TimeUnit.MILLISECONDS.sleep(1000);

    assertEquals(true, scOkMessageSubscriber_3.getFlag());
    log.debug("received 3of3 OkMessage...");

    List<BaseMessage> superconductorEventsSubscriber_3a_Items = new NostrSingleRequestService().send(
        createSuperconductorReqMessageBadgeAwardEvent(Factory.generateRandomHex64String(), voteReceierIdentity.getPublicKey()),
        superconductorRelayUrl);

    // TimeUnit.MILLISECONDS.sleep(2500);

    log.debug("jjjjjjjjjjjjjj");
    log.debug("jjjjjjjjjjjjjj");
    List<EventIF> returnedScBadgeAwardUpvoteEvent_3a = getGenericEvents(superconductorEventsSubscriber_3a_Items);

    assertEquals(returnedScBadgeAwardUpvoteEvent_3a.getFirst().getId(), scBadgeAwardUpvoteEvent_3.getId());
    assertEquals(returnedScBadgeAwardUpvoteEvent_3a.getFirst().getContent(), scBadgeAwardUpvoteEvent_3.getContent());
    assertEquals(returnedScBadgeAwardUpvoteEvent_3a.getFirst().getPublicKey().toHexString(), scBadgeAwardUpvoteEvent_3.getPublicKey().toHexString());
    assertEquals(returnedScBadgeAwardUpvoteEvent_3a.getFirst().getKind(), scBadgeAwardUpvoteEvent_3.getKind());

    // TimeUnit.MILLISECONDS.sleep(1000);

    EventIF eventIF3 = returnedScBadgeAwardUpvoteEvent_3a.stream().max(Comparator.comparing(EventIF::getCreatedAt)).orElseThrow();
    eventServiceIF.processIncomingEvent(
        new EventMessage(eventIF3.asGenericEventRecord()));

// # --------------------- REQ -------------------    
//    submit votes Req to superconductor

    log.debug("kkkkkkkkkkkkkkkk");
    log.debug("kkkkkkkkkkkkkkkk");
    log.debug("problem spot below");

    ReqMessage superconductorReqMessage = createSuperconductorReqMessageBadgeAwardEvent(Factory.generateRandomHex64String(), voteReceierIdentity.getPublicKey());
    log.debug("REQ message:");
    log.debug(Util.prettyFormatJson(superconductorReqMessage.encode()));
    List<BaseMessage> superconductorReqMessageEventsSubscriber_4 = new NostrSingleRequestService().send(
        superconductorReqMessage, superconductorRelayUrl);

    // TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("------------------");

    List<EventIF> returnedVotesFromSc_4 = getGenericEvents(superconductorReqMessageEventsSubscriber_4);

    log.debug("returnedVotesFromSc:");
    returnedVotesFromSc_4.stream().map(EventIF::createPrettyPrintJson).forEach(log::debug);
    log.debug("------------------");

    assertTrue(returnedVotesFromSc_4.stream().map(EventIF::getId).anyMatch(scBadgeAwardUpvoteEvent_3.getId()::equals));
    assertTrue(returnedVotesFromSc_4.stream().map(EventIF::getId).anyMatch(scBadgeAwardUpvoteEvent_3.getId()::equals));
//    assertTrue(Filterable.getTypeSpecificTags(PubKeyTag.class, returnedVotesFromSc.getFirst()).stream().map(PubKeyTag::getPublicKey).map(PublicKey::toHexString).anyMatch(upvote_2.getPublicKey().toString()::equals));
//    assertTrue(Filterable.getTypeSpecificTags(PubKeyTag.class, returnedVotesFromSc.getFirst()).stream().map(PubKeyTag::getPublicKey).map(PublicKey::toHexString).anyMatch(upvote_2.getPublicKey().toString()::equals));
    assertTrue(returnedVotesFromSc_4.stream().map(EventIF::getKind).anyMatch(scBadgeAwardUpvoteEvent_3.getKind()::equals));

    log.debug("llllllllllllllll");
    log.debug("llllllllllllllll");

    // TimeUnit.MILLISECONDS.sleep(1000);

//    query Aimg for (as yet to be impl'd) reputation score event
    List<BaseMessage> afterImageEventsSubscriber_B = new NostrSingleRequestService().send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(), voteReceierIdentity.getPublicKey(),
            definitionsCreatorIdentity.getPublicKey()),
        afterimageRelayUrl);

    log.debug("mmmmmmmmmmmmmmmm");
    log.debug("mmmmmmmmmmmmmmmm");

    // TimeUnit.MILLISECONDS.sleep(1000);

    List<EventIF> returnedAfterImageEvents_B = getGenericEvents(afterImageEventsSubscriber_B);

    // TimeUnit.MILLISECONDS.sleep(1000);

    assertTrue(returnedAfterImageEvents_B.stream().anyMatch(eventIF ->
        Filterable.getTypeSpecificTagsStream(PubKeyTag.class, eventIF)
            .map(PubKeyTag::getPublicKey)
            .anyMatch(publicKey -> publicKey.equals(voteReceierIdentity.getPublicKey()))));

    assertTrue(returnedAfterImageEvents_B.stream().anyMatch(eventIF ->
        Filterable.getTypeSpecificTagsStream(AddressTag.class, eventIF)
            .map(AddressTag::getPublicKey)
            .anyMatch(definitionsCreatorIdentity.getPublicKey()::equals)));

    assertFalse(returnedAfterImageEvents_B.stream().anyMatch(eventIF ->
        Filterable.getTypeSpecificTagsStream(AddressTag.class, eventIF)
            .filter(addressTag -> addressTag.getKind().equals(Kind.BADGE_DEFINITION_EVENT))
            .filter(addressTag -> addressTag.getPublicKey().equals(definitionsCreatorIdentity.getPublicKey()))
            .filter(addressTag -> addressTag.getIdentifierTag().equals(reputationIdentifierTag))
            .toList().isEmpty()));

    assertTrue(returnedAfterImageEvents_B.stream().anyMatch(eventIF ->
        Filterable.getTypeSpecificTagsStream(ExternalIdentityTag.class, eventIF)
            .anyMatch(BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG::equals)));

    log.debug("nnnnnnnnnnnnnnnnnn");
    log.debug("nnnnnnnnnnnnnnnnnn");

    log.debug("returnedAfterImageEvents.size() {}", returnedAfterImageEvents_B.size());
    log.debug("default sort order:");
    returnedAfterImageEvents_B.forEach(eventIF -> log.debug(eventIF.createPrettyPrintJson()));
    log.debug("------");
    returnedAfterImageEvents_B.forEach(eventIF -> log.debug("  {} : {}", eventIF.getContent(), eventIF.getCreatedAt()));

    log.debug("------");
    log.debug("sort order equals presorted? [{}]", returnedAfterImageEvents_B.equals(returnedAfterImageEvents_B.stream().sorted(Comparator.comparing(EventIF::getCreatedAt).reversed()).toList()));

    log.debug("------");
    log.debug("------");
    assertEquals(1, returnedAfterImageEvents_B.size());
    assertEquals("3", returnedAfterImageEvents_B.getFirst().getContent());
  }

  private @NotNull BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> createScUpvoteEvent(Identity voteSubmitterIdentity, Identity voteReceierIdentity) {
    return new BadgeAwardGenericEvent<>(
        voteSubmitterIdentity, // 1111
        voteReceierIdentity.getPublicKey(), // AAAAA
        superconductorRelay,
        awardUpvoteDefinitionEvent);
  }

  private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    List<EventIF> list = returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
    return list;
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
}
