package com.prosilion.afterimage.service.reactive.abstracts;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.service.BaseTestFixtures;
import com.prosilion.afterimage.util.EventAttributesMap;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardCanonicalEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.curated.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.curated.CuratedFormulaEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.nostr.util.Util;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.subdivisions.client.reactive.NostrEventPublisher;
import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public abstract class AbstractWithRelaysTagIT extends BaseTestFixtures {
  protected final List<EventAttributesMap<FormulaEvent>> formulaEventList;
  protected List<EventAttributesMap<BadgeDefinitionGenericEvent>> badgeDefinitionGenericEventList;
  protected final List<EventAttributesMap<CuratedFormulaEvent>> dbCuratedFormulaEventList;

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

  protected final Identity afterimageInstanceIdentity;
  CacheServiceIF cacheServiceIF;

  public AbstractWithRelaysTagIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull String superconductorRelayUrl,
     @NonNull String afterimageRelayUrl,
     CacheServiceIF cacheServiceIF) {
    super(afterimageInstanceIdentity);
    this.cacheServiceIF = cacheServiceIF;

    log.debug("afterimageInstanceIdentity: [{}]", afterimageInstanceIdentity.getPublicKey());
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.superconductorRelayUrl = superconductorRelayUrl;
    this.afterimageRelayUrl = afterimageRelayUrl;
    this.superconductorRelay = new Relay(superconductorRelayUrl);

//  SUPERCONDUCTOR section
    this.badgeDefinitionGenericEventList = createBadgeDefinitionGenericEventList();
    setupBadgeDefinitionEvents(badgeDefinitionGenericEventList);

    this.formulaEventList = createFormulaEventList();
    this.dbCuratedFormulaEventList = setupFormulaEvents(formulaEventList);

//  AIMG section
    submitAimgEvent(
       createBadgeDefinitionReputationEvent());
    log.debug("ctor finished");
  }

  private void setupBadgeDefinitionEvents(List<EventAttributesMap<BadgeDefinitionGenericEvent>> badgeDefinitionGenericEventList) {
    List<BadgeDefinitionGenericEvent> eventList = EventAttributesMap.asEventList(badgeDefinitionGenericEventList);
    eventList
       .forEach(badgeDefinitionGenericEvent ->
          assertTrue(
             new NostrEventPublisher(superconductorRelayUrl)
                .send(
                   new EventMessage(badgeDefinitionGenericEvent)).getFlag()));

    validateSetupCorrectlyCreatedAndPersistedCurationSetsBadgeDefinitionEvents();
  }

  protected void validateSetupCorrectlyCreatedAndPersistedCurationSetsBadgeDefinitionEvents() {
    List<EventIF> sanityCheckReturnedBadgeDefinitionEvents = getEventIFs(
       new NostrSingleRequestService().send(
          new ReqMessage(
             Util.generateRandomHex64String(),
             new Filters(new KindFilter(Kind.BADGE_DEFINITION_EVENT))),
          superconductorRelayUrl));

    log.debug("returned BadgeDefinitionEvents:");
    log.debug("  {}", sanityCheckReturnedBadgeDefinitionEvents.stream().map(EventIF::createPrettyPrintJson).collect(Collectors.joining(",\n")));

    Set<String> sanityCheckCurationSetsBadgeDefinitionEventIds =
       sanityCheckReturnedBadgeDefinitionEvents.stream()
          .map(EventIF::getId)
          .collect(Collectors.toSet());

    Predicate<String> contains = EventAttributesMap.asEventList(this.badgeDefinitionGenericEventList).stream().map(EventIF::getId).toList()::contains;
    assertTrue(sanityCheckCurationSetsBadgeDefinitionEventIds.stream().anyMatch(contains));
  }

  protected List<EventAttributesMap<BadgeDefinitionGenericEvent>> createBadgeDefinitionGenericEventList() {
    return
       EventAttributesMap.asEventAttributesMap(List.of(
          createBadgeAwardUpvoteDefinitionEvent(new Relay(superconductorRelayUrl))
          ,
          createBadgeAwardDownvoteDefinitionEvent(new Relay(superconductorRelayUrl))
       ));
  }

  protected List<EventAttributesMap<FormulaEvent>> createFormulaEventList() {
    List<EventAttributesMap<FormulaEvent>> eventAttributesMap = EventAttributesMap.asEventAttributesMap(
       List.of(
          createPlusOneFormulaEvent()
          ,
          createMinusOneFormulaEvent()
       ));
    return eventAttributesMap;
  }

  private List<EventAttributesMap<CuratedFormulaEvent>> setupFormulaEvents(List<EventAttributesMap<FormulaEvent>> formulaEvents) {
    formulaEvents.forEach(formulaEvent -> assertTrue(
       new NostrEventPublisher(afterimageRelayUrl)
          .send(
             new EventMessage(formulaEvent.getEvent())
             ,
             Duration.ofSeconds(10)
          ).getFlag()));

    List<EventAttributesMap<CuratedFormulaEvent>> list = getDbCuratedFormulaEventList().stream()
       .map(curatedFormulaEvent ->
          new EventAttributesMap<>(
             curatedFormulaEvent,
             curatedFormulaEvent.getFormulaEventCreatorPublicKey(),
             curatedFormulaEvent.getIdentifierTag())).toList();

    return list;
  }

  public List<CuratedFormulaEvent> getDbCuratedFormulaEventList() {
    List<EventIF> sanityCheckReturnedCuratedFormulaEventIFs = getEventIFs(
       new NostrSingleRequestService().send(
          new ReqMessage(
             Util.generateRandomHex64String(),
             new Filters(
                new KindFilter(Kind.CURATION_SETS_FORMULA_EVENT))),
          afterimageRelayUrl));

    log.debug("returned events:");
    log.debug("  {}", sanityCheckReturnedCuratedFormulaEventIFs.stream().map(EventIF::createPrettyPrintJson).collect(Collectors.joining(",\n")));

    Set<String> sanityCheckFormulaEventIds = sanityCheckReturnedCuratedFormulaEventIFs.stream()
       .map(eventIF -> eventIF.getTypeSpecificTags(EventTag.class))
       .flatMap(Collection::stream)
       .map(EventTag::getEventId)
       .collect(Collectors.toSet());

    assertTrue(sanityCheckFormulaEventIds.stream().anyMatch(
       EventAttributesMap.asEventList(formulaEventList).stream().map(FormulaEvent::getId)
          .toList()::contains));

    return sanityCheckReturnedCuratedFormulaEventIFs.stream().map(eventIF ->
       new CuratedFormulaEvent(eventIF.asGenericEventRecord())).toList();
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
       createSuperconductorReqMessageEvent(Util.generateRandomHex64String(), filters), url
//       , Duration.ofMinutes(5)
       , Duration.ofSeconds(10)
    );

    // TimeUnit.MILLISECONDS.sleep(2500);
    log.debug("retrieved superconductor events:");
    List<EventIF> receivedEventIFs = getEventIFs(baseMessages);
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

  protected void submitRelayEvent_WithDuration(EventIF event, String url) {
    assertEquals(true, new NostrEventPublisher(url).send(new EventMessage(event.asGenericEventRecord()), Duration.ofMinutes(30)).getFlag());
//    TimeUnit.MILLISECONDS.sleep(Duration.ofSeconds(10).toMillis());
  }

  protected void submitAimgEvent(EventIF eventIF) {
    submitRelayEvent_WithDuration(eventIF, afterimageRelayUrl);
//    TimeUnit.MILLISECONDS.sleep(1000);
  }

  protected void submitAimgEvent_WithDuration(EventIF eventIF) {
    submitRelayEvent_WithDuration(eventIF, afterimageRelayUrl);
  }

  protected ReqMessage createSuperconductorReqMessageEvent(String subscriberId, Filters filters) {
    return new ReqMessage(subscriberId, filters);
  }

  protected List<EventIF> submitAfterImageReq(PubKeyTag recipientPubKeyTag, String url) {
    log.debug("query Aimg for badgeAwardUpvoteEvent:");
    List<BaseMessage> subscriber = new NostrSingleRequestService().send(
       createAfterImageReqMessage(
          Util.generateRandomHex64String(),
          recipientPubKeyTag),
       url
       , Duration.ofMinutes(30)
    );

    log.debug("afterimage returned events:");
    return getEventIFs(subscriber);
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
//       new AddressTagFilter(
//          new AddressTag(
//             Kind.BADGE_DEFINITION_EVENT,
//             repDefnCreator.getPublicKey(),
//             reputationIdentifierTag)),
       new ReferencedPublicKeyFilter(
          recipientPubKeyTag)
//       ,
//       new ExternalIdentityTagFilter(
//          BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG)
    );
  }

  protected void submitAfterImageReqWithSubscriber(PubKeyTag recipientPubKeyTag, String url, RequestSubscriber<BaseMessage> subscriber) {
    new NostrSingleRequestService().send(
       createAfterImageReqMessage(
          Util.generateRandomHex64String(),
          recipientPubKeyTag),
       url, subscriber);
  }

  protected BadgeAwardCanonicalEvent createUpvoteEventForCanonicalRecipient(Relay... relay) {
    return createUpvoteEvent(recipient.getPublicKey(), relay);
  }

  protected BadgeAwardCanonicalEvent createUpvoteEvent(PublicKey recipientPublicKey, Relay... relay) {
    return new BadgeAwardCanonicalEvent(
       submitter,
       recipientPublicKey,
       EventAttributesMap.getFirstByIdentifierTag(
          this.badgeDefinitionGenericEventList, upvoteIdentifierTag),
       relay);
  }

  protected BadgeAwardCanonicalEvent createDownvoteEventForCanonicalRecipient(Relay... relay) {
    return createDownvoteEvent(recipient.getPublicKey(), relay);
  }

  protected BadgeAwardCanonicalEvent createDownvoteEvent(PublicKey recipientPublicKey, Relay... relay) {
    return new BadgeAwardCanonicalEvent(
       submitter,
       recipientPublicKey,
       EventAttributesMap.getFirstByIdentifierTag(
          this.badgeDefinitionGenericEventList, downvoteIdentifierTag),
       relay);
  }

  protected BadgeDefinitionGenericEvent createBadgeAwardUpvoteDefinitionEvent(Relay... relay) {
    return new BadgeDefinitionGenericEvent(
       upvoteAndOrDownvoteDefnCreator,
       upvoteIdentifierTag,
       String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteAndOrDownvoteDefnCreator.getPublicKey()),
       relay);
  }

  protected BadgeDefinitionGenericEvent createBadgeAwardDownvoteDefinitionEvent(Relay... relay) {
    return new BadgeDefinitionGenericEvent(
       upvoteAndOrDownvoteDefnCreator,
       downvoteIdentifierTag,
       String.format("awardDownvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteAndOrDownvoteDefnCreator.getPublicKey()),
       relay);
  }

  protected FormulaEvent createPlusOneFormulaEvent() {
    return new FormulaEvent(
       formulaCreator,
       formulaUpvoteIdentifierTag,
       EventAttributesMap.getFirstByIdentifierTag(
          this.badgeDefinitionGenericEventList, upvoteIdentifierTag),
       PLUS_ONE_FORMULA,
       superconductorRelay);
  }

  protected FormulaEvent createMinusOneFormulaEvent() {
    return new FormulaEvent(
       formulaCreator,
       formulaDownvoteIdentifierTag,
       EventAttributesMap.getFirstByIdentifierTag(
          this.badgeDefinitionGenericEventList, downvoteIdentifierTag),
       MINUS_ONE_FORMULA,
       superconductorRelay);
  }

  protected BadgeDefinitionReputationEvent createBadgeDefinitionReputationEvent() {
    return new BadgeDefinitionReputationEvent(
       repDefnCreator,
       afterimageInstanceIdentity.getPublicKey(),
       reputationIdentifierTag,
       AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       new Relay(afterimageRelayUrl),
       EventAttributesMap.asEventList(this.dbCuratedFormulaEventList));
  }

//  protected BaseEvent createSearchRelaysListEventMessage() {
//    Util.debug(log, "createSearchRelaysListEventMessage to url:  {}", superconductorRelay.getUrl(), true, '1');
//    return new SearchRelaysListEvent(
//       Identity.generateRandomIdentity(),
//       new RelaysTag(superconductorRelay),
//       "Search Relays List sent from aImg IT 5556");
//  }

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
       getEventIFs(
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

  protected List<EventIF> getEventIFs(List<BaseMessage> messages) {
    return messages.stream()
       .filter(EventMessage.class::isInstance)
       .map(EventMessage.class::cast)
       .map(EventMessage::getEvent)
       .toList();
  }
}
