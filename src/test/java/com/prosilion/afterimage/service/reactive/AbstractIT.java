package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.AuthorFilter;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.filter.tag.ExternalIdentityTagFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.subdivisions.client.reactive.NostrEventPublisher;
import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
import com.prosilion.superconductor.base.service.event.EventServiceIF;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public abstract class AbstractIT {

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

  protected final IdentifierTag reputationIdentifierTag = new IdentifierTag(REPUTATION);
  protected final IdentifierTag upvoteIdentifierTag = new IdentifierTag(AWARD_UNIT_UPVOTE);
  protected final IdentifierTag formulaIdentifierTag = new IdentifierTag(FORMULA_UNIT_UPVOTE);

  protected final Identity defnCreator =
//      Identity.generateRandomIdentity();
      Identity.create("bbb4585483196998204846989544737603523651520600328805626488477202");

  protected final Identity submitter =
//        Identity.generateRandomIdentity();
      Identity.create("aaa4585483196998204846989544737603523651520600328805626488477202");

  protected final Identity recipient =
//        Identity.generateRandomIdentity();
      Identity.create("ccc4585483196998204846989544737603523651520600328805626488477202");

  protected final EventServiceIF eventServiceIF;

  protected final BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent;

  protected final String superconductorRelayUrl;
  protected final String afterimageRelayUrl;
  protected final Relay superconductorRelay;

  public AbstractIT(
      @NonNull @Qualifier("eventService") EventServiceIF eventServiceIF,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) throws ParseException, IOException, InterruptedException {
    this.superconductorRelayUrl = superconductorRelayUrl;
    this.afterimageRelayUrl = afterimageRelayUrl;
    this.eventServiceIF = eventServiceIF;

    Relay afterimageRelay = new Relay(afterimageRelayUrl);
    this.superconductorRelay = new Relay(superconductorRelayUrl);

    log.debug("definitionsCreatorIdentity: [{}]", defnCreator.getPublicKey().toHexString());
    log.debug("voteSubmitterIdentity: [{}]", submitter.getPublicKey().toHexString());

////////////////////////////////
//    START SUPERCONDUCTOR section
//    create then submit SC awardUpvoteDefinitionEvent
    awardUpvoteDefinitionEvent = new BadgeDefinitionGenericEvent(
        defnCreator,
        upvoteIdentifierTag,
        superconductorRelay,
        String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", defnCreator.getPublicKey()));

    log.debug("sending awardUpvoteDefinitionEvent {} to superconductorRelayUrl", awardUpvoteDefinitionEvent);
    RequestSubscriber<OkMessage> awardUpvoteDefinitionEventSubscriber = new RequestSubscriber<>();
    NostrEventPublisher superconductorRelayClient = new NostrEventPublisher(superconductorRelayUrl);
    superconductorRelayClient.send(
        new EventMessage(awardUpvoteDefinitionEvent), awardUpvoteDefinitionEventSubscriber);
    assertEquals(true, awardUpvoteDefinitionEventSubscriber.getItems().getFirst().getFlag());

//    validate SC awardUpvoteDefinitionEvent item
    List<BaseMessage> scUpvoteDefinitionEventMessageItems = new NostrSingleRequestService().send(
        createSuperconductorReqMessageBadgeAwardDefinitionEvent(
            Factory.generateRandomHex64String(),
            defnCreator.getPublicKey()),
        superconductorRelayUrl);

    List<EventIF> returnedSuperconductorAwardUpvoteDefinitionEvents = getGenericEvents(scUpvoteDefinitionEventMessageItems);

    assertEquals(returnedSuperconductorAwardUpvoteDefinitionEvents.getFirst().getId(), awardUpvoteDefinitionEvent.getId());
    assertEquals(returnedSuperconductorAwardUpvoteDefinitionEvents.getFirst().getContent(), awardUpvoteDefinitionEvent.getContent());
    assertEquals(returnedSuperconductorAwardUpvoteDefinitionEvents.getFirst().getPublicKey().toHexString(), awardUpvoteDefinitionEvent.getPublicKey().toHexString());
    assertEquals(returnedSuperconductorAwardUpvoteDefinitionEvents.getFirst().getKind(), awardUpvoteDefinitionEvent.getKind());

//    create then submit SC FormulaEvent
    FormulaEvent plusOneFormulaEvent = new FormulaEvent(
        defnCreator,
        formulaIdentifierTag,
        superconductorRelay,
        awardUpvoteDefinitionEvent,
        PLUS_ONE_FORMULA);
    log.debug("creator public key:\n\n  {}\n\n", defnCreator.getPublicKey().toHexString());
    NostrEventPublisher plusOneFormulaEventSCPublisher = new NostrEventPublisher(superconductorRelayUrl);
    plusOneFormulaEventSCPublisher.send(new EventMessage(plusOneFormulaEvent));

//    validate SC FormulaEvent item
    List<BaseMessage> returnedSCFormulaEventsBaseMessages = new NostrSingleRequestService().send(
        createSuperconductorReqMessageFormulaEvent(
            Factory.generateRandomHex64String(),
            defnCreator.getPublicKey(),
            formulaIdentifierTag),
        superconductorRelayUrl);

    List<EventIF> returnedSCFormulaEvents = getGenericEvents(returnedSCFormulaEventsBaseMessages);
    assertEquals(returnedSCFormulaEvents.getFirst().getId(), plusOneFormulaEvent.getId());
    assertEquals(returnedSCFormulaEvents.getFirst().getContent(), plusOneFormulaEvent.getContent());
    assertEquals(returnedSCFormulaEvents.getFirst().getPublicKey().toHexString(), plusOneFormulaEvent.getPublicKey().toHexString());
    assertEquals(returnedSCFormulaEvents.getFirst().getKind(), plusOneFormulaEvent.getKind());

//    create then submit SC BadgeDefinitionReputationEvent item
    BadgeDefinitionReputationEvent badgeDefinitionReputationEventPlusOneFormula = new BadgeDefinitionReputationEvent(
        defnCreator,
        submitter.getPublicKey(),
        reputationIdentifierTag,
        afterimageRelay,
        AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
        plusOneFormulaEvent);

    eventServiceIF.processIncomingEvent(
        new EventMessage(badgeDefinitionReputationEventPlusOneFormula.asGenericEventRecord()));
    TimeUnit.MILLISECONDS.sleep(1000);
  }

  protected void simulateAimgFollowSetsHandler(EventIF eventIF) {
    eventServiceIF.processIncomingEvent(new EventMessage(eventIF.asGenericEventRecord()));
  }

  protected EventIF submitSCEvent(BaseEvent event, String url, Identity rewardRecipient) {
    //  begin SC votes submissions  

//  submit first Event to superconductor
    submitAimgEvent(event, url);

//    validate by submit Req for above badgeAwardUpvoteEvent to superconductor
    List<BaseMessage> baseMessages =
        new NostrSingleRequestService().send(
            createSuperconductorReqMessageBadgeAwardEvent(
                Factory.generateRandomHex64String(), rewardRecipient.getPublicKey()),
            url);

    // TimeUnit.MILLISECONDS.sleep(2500);
    log.debug("retrieved superconductor events:");
    List<EventIF> receivedEventIFs = getGenericEvents(baseMessages);
    receivedEventIFs.stream().map(EventIF::asGenericEventRecord).map(GenericEventRecord::createPrettyPrintJson).forEach(log::debug);

    EventIF upvoteEventIF = receivedEventIFs.getFirst();

    assertEquals(upvoteEventIF.getId(), event.getId());
    assertEquals(upvoteEventIF.getContent(), event.getContent());
    assertEquals(upvoteEventIF.getPublicKey().toHexString(), event.getPublicKey().toHexString());
    assertEquals(upvoteEventIF.getKind(), event.getKind());

    return upvoteEventIF;
  }

  protected void submitAimgEvent(BaseEvent event, String url) {
    assertEquals(true, new NostrEventPublisher(url).send(new EventMessage(event)).getFlag());
  }

  protected BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> createUpvoteEvent(Identity submitter, Identity recipient, Relay relay) {
    return new BadgeAwardGenericEvent<>(submitter, recipient.getPublicKey(), relay, awardUpvoteDefinitionEvent);
  }

  protected ReqMessage createSuperconductorReqMessageBadgeAwardEvent(String subscriberId, PublicKey recipientPublicKey) {
    return new ReqMessage(subscriberId,
        new Filters(
            new ReferencedPublicKeyFilter(new PubKeyTag(recipientPublicKey)),
            new KindFilter(Kind.BADGE_AWARD_EVENT)));
  }

  private ReqMessage createSuperconductorReqMessageBadgeAwardDefinitionEvent(String subscriberId, PublicKey defnCreatorPublicKey) {
    return new ReqMessage(subscriberId,
        new Filters(
            new AuthorFilter(defnCreatorPublicKey),
            new KindFilter(Kind.BADGE_DEFINITION_EVENT)));
  }

  private ReqMessage createSuperconductorReqMessageFormulaEvent(String subscriberId, PublicKey defnCreatorPublicKey, IdentifierTag identifierTag) {
    return new ReqMessage(subscriberId,
        new Filters(
            new AuthorFilter(defnCreatorPublicKey),
            new KindFilter(Kind.ARBITRARY_CUSTOM_APP_DATA),
            new IdentifierTagFilter(identifierTag)));
  }

  protected BaseEvent createSearchRelaysListEventMessage(Relay shouldAlwaysBeAnSCRelay) {
    return new SearchRelaysListEvent(
        Identity.generateRandomIdentity(),
        new RelaysTag(shouldAlwaysBeAnSCRelay),
        "Search Relays List sent from aImg IT 5556");
  }

  protected List<EventIF> submitAfterImageReq(Identity recipient, Identity defnCreator, String url) throws JsonProcessingException {
    log.debug("query Aimg for badgeAwardUpvoteEvent:");
    List<BaseMessage> subscriber = new NostrSingleRequestService().send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            recipient.getPublicKey(),
            defnCreator.getPublicKey()),
        url);

    log.debug("afterimage returned events:");
    return getGenericEvents(subscriber);
  }

  protected ReqMessage createAfterImageReqMessage(String subscriberId, PublicKey recipientPublicKey, PublicKey defnCreatorPublicKey) throws JsonProcessingException {
    ExternalIdentityTagFilter externalIdentityTagFilter = new ExternalIdentityTagFilter(BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG);
    return new ReqMessage(
        subscriberId,
        new Filters(
            new KindFilter(
                Kind.BADGE_AWARD_EVENT),
//            new IdentifierTagFilter(reputationIdentifierTag),
            new AddressTagFilter(
                new AddressTag(
                    Kind.BADGE_DEFINITION_EVENT,
                    defnCreatorPublicKey,
                    reputationIdentifierTag)),
            new ReferencedPublicKeyFilter(
                new PubKeyTag(
                    recipientPublicKey)),
            externalIdentityTagFilter));
  }

  protected void submitAfterImageReqWithSubscriber(Identity recipient, Identity defnCreator, String url, RequestSubscriber<BaseMessage> subscriber) throws JsonProcessingException {
    new NostrSingleRequestService().send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            recipient.getPublicKey(),
            defnCreator.getPublicKey()),
        url, subscriber);
  }

  protected List<EventIF> getGenericEvents(List<BaseMessage> messages) {
    return messages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
  }
}
