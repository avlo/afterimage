package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
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
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.subdivisions.client.reactive.NostrEventPublisher;
import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  public static final String AWARD_UNIT_DOWNVOTE = "TEST_UNIT_DOWNVOTE";
  public static final String FORMULA_UNIT_UPVOTE = "FORMULA_UNIT_UPVOTE";
  public static final String FORMULA_UNIT_DOWNVOTE = "FORMULA_UNIT_DOWNVOTE";

  public static final String PLUS_ONE_FORMULA = "+1";
  public static final String MINUS_ONE_FORMULA = "-1";

  protected final IdentifierTag reputationIdentifierTag = new IdentifierTag(REPUTATION);
  protected final IdentifierTag upvoteIdentifierTag = new IdentifierTag(AWARD_UNIT_UPVOTE);
  protected final IdentifierTag downvoteIdentifierTag = new IdentifierTag(AWARD_UNIT_DOWNVOTE);
  protected final IdentifierTag formulaUpvoteIdentifierTag = new IdentifierTag(FORMULA_UNIT_UPVOTE);
  protected final IdentifierTag formulaDownvoteIdentifierTag = new IdentifierTag(FORMULA_UNIT_DOWNVOTE);

  protected final Identity defnCreator =
     Identity.generateRandomIdentity();
//      Identity.create("bbb4585483196998204846989544737603523651520600328805626488477202");

  protected final Identity submitter =
     Identity.generateRandomIdentity();
//      Identity.create("aaa4585483196998204846989544737603523651520600328805626488477202");

  protected final Identity recipient =
     Identity.generateRandomIdentity();
//      Identity.create("ccc4585483196998204846989544737603523651520600328805626488477202");

  protected final BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent;
  protected final BadgeDefinitionGenericEvent awardDownvoteDefinitionEvent;
  protected final FormulaEvent plusOneFormulaEvent;
  protected final FormulaEvent minusOneFormulaEvent;

  protected final String superconductorRelayUrl;
  protected final String afterimageRelayUrl;
  protected final Relay superconductorRelay;

  protected Function<PublicKey, Filters> badgeAwardEventFilter = publicKey -> new Filters(
     new ReferencedPublicKeyFilter(new PubKeyTag(publicKey)), new KindFilter(Kind.BADGE_AWARD_EVENT));

  protected Function<PublicKey, Filters> badgeDefinitionEventFilter = publicKey -> new Filters(
     new AuthorFilter(publicKey), new KindFilter(Kind.BADGE_DEFINITION_EVENT));

  protected BiFunction<PublicKey, IdentifierTag, Filters> formulaEventFilter = (publicKey, identifierTag) ->
     new Filters(new AuthorFilter(publicKey), new KindFilter(Kind.ARBITRARY_CUSTOM_APP_DATA), new IdentifierTagFilter(identifierTag));

  public AbstractIT(
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) throws ParseException, InterruptedException {
    this.superconductorRelayUrl = superconductorRelayUrl;
    this.afterimageRelayUrl = afterimageRelayUrl;
    this.superconductorRelay = new Relay(superconductorRelayUrl);

    log.debug("definitionsCreatorIdentity: [{}]", defnCreator.getPublicKey().toHexString());
    log.debug("voteSubmitterIdentity: [{}]", submitter.getPublicKey().toHexString());

//  SUPERCONDUCTOR section
    awardUpvoteDefinitionEvent = createBadgeAwardUpvoteDefinitionEvent(defnCreator);
    submitSCEvent(awardUpvoteDefinitionEvent, superconductorRelayUrl, badgeDefinitionEventFilter.apply(defnCreator.getPublicKey()));

    this.plusOneFormulaEvent = createFormulaUpvoteEvent(defnCreator);
    submitSCEvent(plusOneFormulaEvent, superconductorRelayUrl, formulaEventFilter.apply(defnCreator.getPublicKey(), formulaUpvoteIdentifierTag));

    awardDownvoteDefinitionEvent = createBadgeAwardDownvoteDefinitionEvent(defnCreator);
    submitSCEvent(awardDownvoteDefinitionEvent, superconductorRelayUrl, badgeDefinitionEventFilter.apply(defnCreator.getPublicKey()));

    this.minusOneFormulaEvent = createFormulaDownvoteEvent(defnCreator);
    submitSCEvent(minusOneFormulaEvent, superconductorRelayUrl, formulaEventFilter.apply(defnCreator.getPublicKey(), formulaDownvoteIdentifierTag));

//  AIMG section
    submitAimgEvent(
       createBadgeDefinitionReputationEvent(defnCreator, submitter, new Relay(afterimageRelayUrl), plusOneFormulaEvent, minusOneFormulaEvent));
    TimeUnit.MILLISECONDS.sleep(1000);
  }

  protected EventIF submitSCEvent(BaseEvent event, String url, Filters filters) {
//  submit first Event to superconductor
    submitRelayEvent(event, url);

//    validate by submit Req for above badgeAwardUpvoteEvent to superconductor
    List<BaseMessage> baseMessages = new NostrSingleRequestService().send(
       createSuperconductorReqMessageEvent(Factory.generateRandomHex64String(), filters), url);

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

  protected void submitRelayEvent(EventIF event, String url) {
    assertEquals(true, new NostrEventPublisher(url)
       .send(new EventMessage(event.asGenericEventRecord())).getFlag());
  }

  protected void submitAimgEvent(EventIF eventIF) {
    submitRelayEvent(eventIF, afterimageRelayUrl);
  }

  protected ReqMessage createSuperconductorReqMessageEvent(String subscriberId, Filters filters) {
    return new ReqMessage(subscriberId, filters);
  }

  protected List<EventIF> submitAfterImageReq(PublicKey defnCreator, PubKeyTag recipientPubKeyTag, String url) {
    log.debug("query Aimg for badgeAwardUpvoteEvent:");
    List<BaseMessage> subscriber = new NostrSingleRequestService().send(
       createAfterImageReqMessage(
          Factory.generateRandomHex64String(),
          defnCreator,
          recipientPubKeyTag),
       url);

    log.debug("afterimage returned events:");
    return getGenericEvents(subscriber);
  }

  protected ReqMessage createAfterImageReqMessage(String subscriberId, PublicKey defnCreatorPublicKey, PubKeyTag recipientPubKeyTag) {
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
             recipientPubKeyTag),
          externalIdentityTagFilter));
  }

  protected void submitAfterImageReqWithSubscriber(PublicKey defnCreator, PubKeyTag recipientPubKeyTag, String url, RequestSubscriber<BaseMessage> subscriber) {
    new NostrSingleRequestService().send(
       createAfterImageReqMessage(
          Factory.generateRandomHex64String(),
          defnCreator,
          recipientPubKeyTag),
       url, subscriber);
  }

  protected BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> createUpvoteEvent(Identity submitter, Identity recipient, Relay relay) {
    return new BadgeAwardGenericEvent<>(submitter, recipient.getPublicKey(), relay, awardUpvoteDefinitionEvent);
  }

  protected BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> createDownvoteEvent(Identity submitter, Identity recipient, Relay relay) {
    return new BadgeAwardGenericEvent<>(submitter, recipient.getPublicKey(), relay, awardDownvoteDefinitionEvent);
  }

  protected BadgeDefinitionGenericEvent createBadgeAwardUpvoteDefinitionEvent(Identity defnCreator) {
    return new BadgeDefinitionGenericEvent(
       defnCreator,
       upvoteIdentifierTag,
       superconductorRelay,
       String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", defnCreator.getPublicKey()));
  }

  protected BadgeDefinitionGenericEvent createBadgeAwardDownvoteDefinitionEvent(Identity defnCreator) {
    return new BadgeDefinitionGenericEvent(
       defnCreator,
       downvoteIdentifierTag,
       superconductorRelay,
       String.format("awardDownvoteDefinitionEvent, definition creator PublicKey: [%s]", defnCreator.getPublicKey()));
  }

  protected FormulaEvent createFormulaUpvoteEvent(Identity defnCreator) throws ParseException {
    return new FormulaEvent(
       defnCreator,
       formulaUpvoteIdentifierTag,
       superconductorRelay,
       awardUpvoteDefinitionEvent,
       PLUS_ONE_FORMULA);
  }

  protected FormulaEvent createFormulaDownvoteEvent(Identity defnCreator) throws ParseException {
    return new FormulaEvent(
       defnCreator,
       formulaDownvoteIdentifierTag,
       superconductorRelay,
       awardDownvoteDefinitionEvent,
       MINUS_ONE_FORMULA);
  }

  protected BadgeDefinitionReputationEvent createBadgeDefinitionReputationEvent(
     Identity defnCreator,
     Identity submitter,
     Relay afterimageRelay,
     FormulaEvent... formulaEvents) {
    return new BadgeDefinitionReputationEvent(
       defnCreator,
       submitter.getPublicKey(),
       reputationIdentifierTag,
       afterimageRelay,
       AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       formulaEvents);

  }

  protected BaseEvent createSearchRelaysListEventMessage(Relay shouldAlwaysBeAnSCRelay) {
    return new SearchRelaysListEvent(
       Identity.generateRandomIdentity(),
       new RelaysTag(shouldAlwaysBeAnSCRelay),
       "Search Relays List sent from aImg IT 5556");
  }

  protected List<EventIF> validateGeneralAfterimageRequestResults(List<EventIF> returnedReputationEventIFs) {
    returnedReputationEventIFs.forEach(eventIF -> log.debug(eventIF.getId()));

    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF ->
       eventIF.findFirstTag(PubKeyTag.class).map(PubKeyTag::getPublicKey).stream()
          .anyMatch(recipient.getPublicKey()::equals)));

    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF ->
       eventIF.findFirstTag(AddressTag.class).stream()
          .map(AddressTag::getPublicKey)
          .anyMatch(defnCreator.getPublicKey()::equals)));

    assertFalse(returnedReputationEventIFs.stream().anyMatch(eventIF ->
       eventIF.findFirstTag(AddressTag.class).stream()
          .filter(addressTag -> addressTag.getKind().equals(Kind.BADGE_DEFINITION_EVENT))
          .filter(addressTag -> addressTag.getPublicKey().equals(defnCreator.getPublicKey()))
          .filter(addressTag -> addressTag.getIdentifierTag().equals(reputationIdentifierTag))
          .toList().isEmpty()));

    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF ->
       eventIF.findFirstTag(ExternalIdentityTag.class).stream()
          .anyMatch(BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG::equals)));

    return returnedReputationEventIFs;
  }

  protected List<EventIF> validateSpecificAfterimageRequestResults(RequestSubscriber<BaseMessage> subscriber, int count, String expectedScore) {
    List<EventIF> events =
       validateGeneralAfterimageRequestResults(
          getGenericEvents(subscriber.getItems()));
    assertEquals(count, (long) events.size());
    assertEquals(expectedScore, events.getFirst().getContent());
    return events;
  }

  protected List<EventIF> getGenericEvents(List<BaseMessage> messages) {
    return messages.stream()
       .filter(EventMessage.class::isInstance)
       .map(EventMessage.class::cast)
       .map(EventMessage::getEvent)
       .toList();
  }
}
