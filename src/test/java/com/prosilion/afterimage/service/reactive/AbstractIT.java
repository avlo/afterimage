package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.curated.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.curated.CuratedFormulaEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.filter.tag.ExternalIdentityTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.ReferenceTag;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.nostr.util.Util;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.subdivisions.client.reactive.NostrEventPublisher;
import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

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

  public static final String REPUTATION = "BADGE_DEFN_UNIT_REP";
  public static final String AWARD_UNIT_UPVOTE = "BDG_DEF_UNIT_UP";
  public static final String AWARD_UNIT_DOWNVOTE = "BDG_DEF_UNIT_DOWN";
  public static final String FORMULA_UNIT_UPVOTE = "FORMULA_UNIT_UPVOTE";
  public static final String FORMULA_UNIT_DOWNVOTE = "FORMULA_UNIT_DOWNVOTE";

  public static final String PLUS_ONE_FORMULA = "+1";
  public static final String MINUS_ONE_FORMULA = "-1";

  public final static IdentifierTag reputationIdentifierTag = new IdentifierTag(REPUTATION);
  public final static IdentifierTag upvoteIdentifierTag = new IdentifierTag(AWARD_UNIT_UPVOTE);
  public final static IdentifierTag downvoteIdentifierTag = new IdentifierTag(AWARD_UNIT_DOWNVOTE);
  public final static IdentifierTag formulaUpvoteIdentifierTag = new IdentifierTag(FORMULA_UNIT_UPVOTE);
  public final static IdentifierTag formulaDownvoteIdentifierTag = new IdentifierTag(FORMULA_UNIT_DOWNVOTE);

  protected final Identity afterimageInstanceIdentity;

  public final static Identity submitter =
//     Identity.generateRandomIdentity();
     Identity.create("aaa4585483196998204846989544737603523651520600328805626488477202");

  public final static Identity upvoteAndOrDownvoteDefnCreator =
//     Identity.generateRandomIdentity();
     Identity.create("bbb4585483196998204846989544737603523651520600328805626488477202");

  public final static Identity recipient =
//     Identity.generateRandomIdentity();
     Identity.create("ccc4585483196998204846989544737603523651520600328805626488477202");

  public final static Identity formulaCreator =
//     Identity.generateRandomIdentity();
     Identity.create("ddd4585483196998204846989544737603523651520600328805626488477202");

  public final static Identity repDefnCreator =
//     Identity.generateRandomIdentity();
     Identity.create("eee4585483196998204846989544737603523651520600328805626488477202");

  protected final BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent;
  protected final BadgeDefinitionGenericEvent awardDownvoteDefinitionEvent;

  protected final FormulaEvent plusOneFormulaEvent;
  protected final FormulaEvent minusOneFormulaEvent;

  protected final CuratedFormulaEvent plusOneCuratedFormulaEvent;
  protected final CuratedFormulaEvent minusOneCuratedFormulaEvent;

  protected final String superconductorRelayUrl;
  protected final String afterimageRelayUrl;
  protected final Relay superconductorRelay;

  protected Filters upvoteAndOrDownvoteEventFilter =
     new Filters(
        new ReferencedPublicKeyFilter(
           new PubKeyTag(recipient.getPublicKey())),
        new KindFilter(Kind.CURATION_SETS_BADGE_AWARD_EVENT));

  protected Filters upvoteAndOrDownvoteDefinitionEventFilter =
     new Filters(
        new ReferencedPublicKeyFilter(
           new PubKeyTag(upvoteAndOrDownvoteDefnCreator.getPublicKey())),
        new KindFilter(Kind.CURATION_SETS_BADGE_DEFINITION_EVENT));

  protected BiFunction<PublicKey, AddressTag, Filters> curatedFormulaEventFilter = (publicKey, addressTag) ->
     new Filters(
        new ReferencedPublicKeyFilter(new PubKeyTag(publicKey)),
        new KindFilter(Kind.CURATION_SETS_FORMULA_EVENT),
        new AddressTagFilter(addressTag));

  public AbstractIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) {
    log.debug("afterimageInstanceIdentity: [{}]", afterimageInstanceIdentity.getPublicKey());
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.superconductorRelayUrl = superconductorRelayUrl;
    this.afterimageRelayUrl = afterimageRelayUrl;
    this.superconductorRelay = new Relay(superconductorRelayUrl);

//  SUPERCONDUCTOR section
    this.awardUpvoteDefinitionEvent = createBadgeAwardUpvoteDefinitionEvent();
    submitSCEvent(awardUpvoteDefinitionEvent, superconductorRelayUrl, upvoteAndOrDownvoteDefinitionEventFilter);
    this.awardDownvoteDefinitionEvent = createBadgeAwardDownvoteDefinitionEvent();
    submitSCEvent(awardDownvoteDefinitionEvent, superconductorRelayUrl, upvoteAndOrDownvoteDefinitionEventFilter);

    this.plusOneFormulaEvent = createPlusOneFormulaEvent();
    this.plusOneCuratedFormulaEvent = createCuratedFormulaPlusOneEvent();
    submitPlusOneFormulaEventToImplSpecificRelay();

    this.minusOneFormulaEvent = createMinusOneFormulaEvent();
    this.minusOneCuratedFormulaEvent = createCuratedFormulaMinusOneEvent();
    submitDownvoteFormulaEventToImplSpecificRelay();

//  AIMG section
    submitAimgEvent(
       createBadgeDefinitionReputationEvent());
    log.debug("ctor finished");
  }

  protected void submitPlusOneFormulaEventToImplSpecificRelay() {
    submitSCEvent(
       plusOneFormulaEvent,
       superconductorRelayUrl,
       curatedFormulaEventFilter.apply(
          formulaCreator.getPublicKey(),
          awardUpvoteDefinitionEvent.asAddressableEventAddressTag()));
  }

  protected void submitDownvoteFormulaEventToImplSpecificRelay() {
    submitSCEvent(
       minusOneFormulaEvent,
       superconductorRelayUrl,
       curatedFormulaEventFilter.apply(
          formulaCreator.getPublicKey(),
          awardDownvoteDefinitionEvent.asAddressableEventAddressTag()));
  }

  protected EventIF submitSCEvent(BaseEvent event, String url, Filters filters) {
//  submit first Event to superconductor
    submitRelayEvent(event, url);
//  sanity check event submissions processed by superconductor
//    List<BaseMessage> baseMessages = new NostrSingleRequestService().send(
//       createSuperconductorReqMessageEvent(generateRandomHex64String(), filters), url);
//
//    // TimeUnit.MILLISECONDS.sleep(2500);
//    log.debug("retrieved superconductor events:");
//    List<EventIF> receivedEventIFs = getGenericEvents(baseMessages);
//    receivedEventIFs.stream().map(EventIF::asGenericEventRecord).map(GenericEventRecord::createPrettyPrintJson).forEach(log::debug);
//
//    EventIF upvoteEventIF = receivedEventIFs.getFirst();
//
//    assertEquals(upvoteEventIF.getId(), event.getId());
//    assertEquals(upvoteEventIF.getContent(), event.getContent());
//    assertEquals(upvoteEventIF.getPublicKey().toHexString(), event.getPublicKey().toHexString());
//    assertEquals(upvoteEventIF.getKind(), event.getKind());
//
//    return upvoteEventIF;
    return event;
  }

  protected EventIF submitSCEventWithDuration_backup(BaseEvent event, String url, Filters filters) {
//  submit first Event to superconductor
    submitRelayEvent(event, url);
//  sanity check event submissions processed by superconductor
    List<BaseMessage> baseMessages = new NostrSingleRequestService().send(
       createSuperconductorReqMessageEvent(generateRandomHex64String(), filters), url
//       , Duration.ofMinutes(5)
       , Duration.ofSeconds(10)
    );

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
    assertEquals(true, new NostrEventPublisher(url).send(new EventMessage(event.asGenericEventRecord())).getFlag());
  }

  protected void submitRelayEventWithDuration_backup(EventIF event, String url) {
    assertEquals(true, new NostrEventPublisher(url).send(new EventMessage(event.asGenericEventRecord()), Duration.ofMinutes(30)).getFlag());
//    TimeUnit.MILLISECONDS.sleep(Duration.ofSeconds(10).toMillis());
  }

  protected void submitAimgEvent(EventIF eventIF) {
    submitRelayEventWithDuration_backup(eventIF, afterimageRelayUrl);
//    TimeUnit.MILLISECONDS.sleep(1000);
  }

  protected void submitAimgEventWithDuration_backup(EventIF eventIF) {
    submitRelayEventWithDuration_backup(eventIF, afterimageRelayUrl);
  }

  protected ReqMessage createSuperconductorReqMessageEvent(String subscriberId, Filters filters) {
    return new ReqMessage(subscriberId, filters);
  }

  protected List<EventIF> submitAfterImageReq(PubKeyTag recipientPubKeyTag, String url) {
    log.debug("query Aimg for badgeAwardUpvoteEvent:");
    List<BaseMessage> subscriber = new NostrSingleRequestService().send(
       createAfterImageReqMessage(
          generateRandomHex64String(),
          recipientPubKeyTag),
       url
       , Duration.ofMinutes(30)
    );

    log.debug("afterimage returned events:");
    return getGenericEvents(subscriber);
  }

  @SneakyThrows
  protected ReqMessage createAfterImageReqMessage(String subscriberId, PubKeyTag recipientPubKeyTag) {
    ReqMessage reqMessage = new ReqMessage(
       subscriberId,
       createBadgeAwardRecipientFilters(recipientPubKeyTag));
    log.debug(Util.prettyFormatJson(reqMessage.encode(), 2));
    return reqMessage;
  }

  protected Filters createBadgeAwardRecipientFilters(PubKeyTag recipientPubKeyTag) {
    return new Filters(
       new KindFilter(
          Kind.BADGE_AWARD_EVENT),
//          new IdentifierTagFilter(
//             new IdentifierTag(defnCreatorPublicKey.toHexString())),
       new AddressTagFilter(
          new AddressTag(
             Kind.BADGE_DEFINITION_EVENT,
             repDefnCreator.getPublicKey(),
             reputationIdentifierTag)),
       new ReferencedPublicKeyFilter(
          recipientPubKeyTag),
       new ExternalIdentityTagFilter(
          BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG));
  }

  protected void submitAfterImageReqWithSubscriber(PubKeyTag recipientPubKeyTag, String url, RequestSubscriber<BaseMessage> subscriber) {
    new NostrSingleRequestService().send(
       createAfterImageReqMessage(
          generateRandomHex64String(),
          recipientPubKeyTag),
       url, subscriber);
  }

  protected BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> createUpvoteEvent(Relay relay) {
    return new BadgeAwardGenericEvent<>(
       submitter,
       recipient.getPublicKey(),
       awardUpvoteDefinitionEvent,
       relay);
  }

  protected BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> createDownvoteEvent(Relay relay) {
    return new BadgeAwardGenericEvent<>(
       submitter,
       recipient.getPublicKey(),
       awardDownvoteDefinitionEvent,
       relay);
  }

  protected BadgeDefinitionGenericEvent createBadgeAwardUpvoteDefinitionEvent() {
    return new BadgeDefinitionGenericEvent(
       upvoteAndOrDownvoteDefnCreator,
       upvoteIdentifierTag,
       String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteAndOrDownvoteDefnCreator.getPublicKey()),
       superconductorRelay);
  }

  protected BadgeDefinitionGenericEvent createBadgeAwardDownvoteDefinitionEvent() {
    return new BadgeDefinitionGenericEvent(
       upvoteAndOrDownvoteDefnCreator,
       downvoteIdentifierTag,
       String.format("awardDownvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteAndOrDownvoteDefnCreator.getPublicKey()),
       superconductorRelay);
  }

  protected FormulaEvent createPlusOneFormulaEvent() {
    return new FormulaEvent(
       formulaCreator,
       formulaUpvoteIdentifierTag,
       awardUpvoteDefinitionEvent,
       PLUS_ONE_FORMULA,
       superconductorRelay);
  }

  protected CuratedFormulaEvent createCuratedFormulaPlusOneEvent() {
    return new CuratedFormulaEvent(
       afterimageInstanceIdentity,
       plusOneFormulaEvent,
       new ReferenceTag(superconductorRelay.getUrl()),
       superconductorRelay);
  }

  protected FormulaEvent createMinusOneFormulaEvent() {
    return new FormulaEvent(
       formulaCreator,
       formulaDownvoteIdentifierTag,
       awardDownvoteDefinitionEvent,
       MINUS_ONE_FORMULA,
       superconductorRelay);
  }

  protected CuratedFormulaEvent createCuratedFormulaMinusOneEvent() {
    return new CuratedFormulaEvent(
       afterimageInstanceIdentity,
       minusOneFormulaEvent,
       new ReferenceTag(superconductorRelay.getUrl()),
       superconductorRelay);
  }

  protected BadgeDefinitionReputationEvent createBadgeDefinitionReputationEvent() {
    return new BadgeDefinitionReputationEvent(
       repDefnCreator,
       afterimageInstanceIdentity.getPublicKey(),
       reputationIdentifierTag,
       AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       new Relay(afterimageRelayUrl),
       plusOneCuratedFormulaEvent, minusOneCuratedFormulaEvent);
  }

  protected BaseEvent createSearchRelaysListEventMessage() {
    Util.debug(log, "createSearchRelaysListEventMessage to url:  {}", superconductorRelay.getUrl(), true, '1');
    return new SearchRelaysListEvent(
       Identity.generateRandomIdentity(),
       new RelaysTag(superconductorRelay),
       "Search Relays List sent from aImg IT 5556");
  }

  protected List<EventIF> validateGeneralAfterimageRequestResults(List<EventIF> returnedReputationEventIFs) {
    assertFalse(returnedReputationEventIFs.isEmpty());

    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF ->
       eventIF.findFirstTag(PubKeyTag.class).map(PubKeyTag::getPublicKey).stream()
          .anyMatch(recipient.getPublicKey()::equals)));

    assertFalse(returnedReputationEventIFs.stream().anyMatch(eventIF ->
       eventIF.findFirstTag(AddressTag.class).stream()
          .filter(addressTag -> addressTag.getKind().equals(Kind.BADGE_DEFINITION_EVENT))
          .filter(addressTag -> addressTag.getPublicKey().equals(repDefnCreator.getPublicKey()))
          .filter(addressTag -> addressTag.requireIdentifierTag().equals(reputationIdentifierTag))
          .toList().isEmpty()));

    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF ->
       eventIF.findFirstTag(ExternalIdentityTag.class).stream()
          .anyMatch(this::isEquals)));

    return returnedReputationEventIFs;
  }

  private boolean isEquals(ExternalIdentityTag externalIdentityTag) {
    log.debug("       incoming          externalIdentityTag:\n {}", externalIdentityTag);
    log.debug("BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG:\n {}", BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG);
    boolean equals = externalIdentityTag.equals(BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG);
    log.debug(String.format("  %s", equals ?
       "+++ MATCH" :
       "--- NO MATCH: " + StringUtils.difference(BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG.toString(), externalIdentityTag.toString())));
    return equals;
  }

  protected List<EventIF> validateSpecificAfterimageRequestResults(RequestSubscriber<BaseMessage> subscriber, int count, String expectedScore) {
    return validateSpecificAfterimageRequestResults(
       getGenericEvents(
          subscriber.getItems()),
       count,
       expectedScore);
  }

  protected List<EventIF> validateBadgeAwardProcessingCompletion(List<GenericEventRecord> genericEventRecords, int count) {
    return validateCount(genericEventRecords.stream().collect(Collectors.toUnmodifiableList()), count);
  }

  protected List<EventIF> validateBadgeAwardProcessingCompletion(List<GenericEventRecord> genericEventRecords, int count, String expectedScore) {
    return validateSpecificAfterimageRequestResults(genericEventRecords.stream().collect(Collectors.toUnmodifiableList()), count, expectedScore);
  }

  protected List<EventIF> validateSpecificAfterimageRequestResults(List<EventIF> events, int count, String expectedScore) {
    validateCount(events, count);
    assertEquals(expectedScore, events.getFirst().getContent());
    return events;
  }

  protected List<EventIF> validateCount(List<EventIF> events, int count) {
    assertEquals(count, (long) events.size());
    return events;
  }

  protected List<EventIF> getGenericEvents(List<BaseMessage> messages) {
    return messages.stream()
       .filter(EventMessage.class::isInstance)
       .map(EventMessage.class::cast)
       .map(EventMessage::getEvent)
       .toList();
  }

  public static String generateRandomHex64String() {
    return UUID.randomUUID().toString().concat(UUID.randomUUID().toString()).replaceAll("[^A-Za-z0-9]", "");
  }
}
