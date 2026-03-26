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

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(SingleContainerTestConfig.class)
public class SuperconductorSingleEventThenAfterimageReqIT {

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
  public SuperconductorSingleEventThenAfterimageReqIT(
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
    RequestSubscriber<OkMessage> awardUpvoteDefinitionEventSubscriber = new RequestSubscriber<>();
    NostrEventPublisher superconductorRelayClient = new NostrEventPublisher(superconductorRelayUrl);
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
    superconductorRelayClient.closeSocket();
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

//  begin SC votes submissions  
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> scBadgeAwardUpvoteEvent_1 = createScUpvoteEvent(voteSubmitterIdentity, voteReceierIdentity);

//  submit first Event to superconductor
    OkMessage items_1 = new NostrEventPublisher(superconductorRelayUrl).send(
        new EventMessage(scBadgeAwardUpvoteEvent_1));

    // TimeUnit.MILLISECONDS.sleep(2500);

    assertEquals(true, items_1.getFlag());

//    validate by submit Req for above badgeAwardUpvoteEvent to superconductor
    List<BaseMessage> superconductorEventsSubscriber_1a_Items =
        new NostrSingleRequestService().send(
            createSuperconductorReqMessageBadgeAwardEvent(
                Factory.generateRandomHex64String(), voteReceierIdentity.getPublicKey()),
            superconductorRelayUrl);

    // TimeUnit.MILLISECONDS.sleep(2500);
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
    log.debug("query Aimg for above badgeAwardUpvoteEvent:");
    List<BaseMessage> aimgSubscriberItems_A = new NostrSingleRequestService().send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            voteReceierIdentity.getPublicKey(),
            definitionsCreatorIdentity.getPublicKey()),
        afterimageRelayUrl);

    log.debug("afterimage returned events:");
    List<EventIF> returnedAimgReqGenericEvents_A = getGenericEvents(aimgSubscriberItems_A);

    assertEquals("1", returnedAimgReqGenericEvents_A.getFirst().getContent());
//    assertTrue(Filterable.getTypeSpecificTags(PubKeyTag.class, returnedAimgReqGenericEvents_A.getFirst()).stream().map(PubKeyTag::getPublicKey).map(PublicKey::toHexString).anyMatch(badgeDefinitionUpvoteCreatorPubkey_3333.getPublicKey().toString()::equals));
    assertEquals(returnedAimgReqGenericEvents_A.getFirst().getKind(), scBadgeAwardUpvoteEvent_1.getKind());
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
