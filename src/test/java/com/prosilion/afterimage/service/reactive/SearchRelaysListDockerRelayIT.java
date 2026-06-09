package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.config.MultiContainerTestConfig;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
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
import com.prosilion.nostr.util.Util;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.subdivisions.client.reactive.NostrEventPublisher;
import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static com.prosilion.afterimage.config.MultiContainerTestConfig.AFTERIMAGE_APP_TWO;
import static com.prosilion.afterimage.config.MultiContainerTestConfig.SUPERCONDUCTOR_AFTERIMAGE;
import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * test name "SearchRelaysListRelaySets" means:
 * BadgeDefinitionReputationEvent and SearchRelaysListEvent for docker (5557) aImg relay
 * note: varies from {@link SearchRelaysListSameRelayIT}, which is 5556
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(MultiContainerTestConfig.class)
public class SearchRelaysListDockerRelayIT {
  public static final String REPUTATION = "TEST_REPUTATION";
  public static final String AWARD_UNIT_UPVOTE = "TEST_UNIT_UPVOTE";
  public static final String FORMULA_UNIT_UPVOTE = "FORMULA_UNIT_UPVOTE";

  public static final String PLUS_ONE_FORMULA = "+1";

  protected final IdentifierTag reputationIdentifierTag = new IdentifierTag(REPUTATION);
  protected final IdentifierTag upvoteIdentifierTag = new IdentifierTag(AWARD_UNIT_UPVOTE);
  protected final IdentifierTag formulaUpvoteIdentifierTag = new IdentifierTag(FORMULA_UNIT_UPVOTE);

  protected final Identity defnCreator =
     Identity.generateRandomIdentity();
//      Identity.create("bbb4585483196998204846989544737603523651520600328805626488477202");

  protected final Identity submitter =
     Identity.generateRandomIdentity();
//      Identity.create("aaa4585483196998204846989544737603523651520600328805626488477202");

  protected final Identity recipient =
     Identity.generateRandomIdentity();
//      Identity.create("ccc4585483196998204846989544737603523651520600328805626488477202");

  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventPlusOneFormula;

  private final Relay superconductorRelayTagUrl;
  private final Relay afterimageRelayTagUrlTwo;

  private final String superconductorRelayUrl;
  private final String afterimageRelayUrlTwo;

  private final BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent;

  @Autowired
  public SearchRelaysListDockerRelayIT(
     @NonNull @Value("${afterimage.relay.url.two}") String afterimageRelayUrlTwo,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl) throws ParseException, InterruptedException {
    this.superconductorRelayUrl = superconductorRelayUrl;
    this.afterimageRelayUrlTwo = afterimageRelayUrlTwo;

    superconductorRelayTagUrl = new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555");
    afterimageRelayTagUrlTwo = new Relay("ws://" + AFTERIMAGE_APP_TWO + ":5556");

    awardUpvoteDefinitionEvent = new BadgeDefinitionGenericEvent(
       defnCreator,
       upvoteIdentifierTag,
       superconductorRelayTagUrl,
       String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", defnCreator.getPublicKey()));

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent =
       new BadgeAwardGenericEvent<>(
          submitter,
          recipient.getPublicKey(),
          superconductorRelayTagUrl,
          awardUpvoteDefinitionEvent,
          String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", recipient.getPublicKey()));

    FormulaEvent plusOneFormulaEvent = new FormulaEvent(
       defnCreator,
       formulaUpvoteIdentifierTag,
       superconductorRelayTagUrl,
       awardUpvoteDefinitionEvent,
       PLUS_ONE_FORMULA);

    badgeDefinitionReputationEventPlusOneFormula = new BadgeDefinitionReputationEvent(
       defnCreator,
       submitter.getPublicKey(),
       reputationIdentifierTag,
       afterimageRelayTagUrlTwo,
       AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       plusOneFormulaEvent);

    log.debug("1of5 - sendEventToSuperconductor(awardUpvoteDefinitionEvent)");
    submitRelayEvent(awardUpvoteDefinitionEvent, superconductorRelayUrl);
    TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("2of5 - sendEventToSuperconductor(badgeAwardUpvoteEvent)");
    submitRelayEvent(badgeAwardUpvoteEvent, superconductorRelayUrl);
    TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("3of5 - sendEventToSuperconductor(plusOneFormulaEvent)");
    submitRelayEvent(plusOneFormulaEvent, superconductorRelayUrl);
    TimeUnit.MILLISECONDS.sleep(1000);

//  AIMG section		
    log.debug("4of5 - sendEventToSuperconductor(badgeDefinitionReputationEventPlusOneFormula)");
    submitRelayEvent(badgeDefinitionReputationEventPlusOneFormula, afterimageRelayUrlTwo);
    TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("5of5 - submitRelayEvent(createSearchRelaysListEventMessage(),afterimageRelayUrlTwo)");
    submitRelayEvent(
       createSearchRelaysListEventMessage(),
       afterimageRelayUrlTwo);
    TimeUnit.MILLISECONDS.sleep(1000);
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException, InterruptedException {
// aImg_2 sanity check		
    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_A = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(defnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlTwo, aImg_2_EventSubscriber_A);

    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_A, 1, "1");

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent_2 =
       new BadgeAwardGenericEvent<>(
          submitter,
          recipient.getPublicKey(),
          superconductorRelayTagUrl,
          awardUpvoteDefinitionEvent,
          String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", recipient.getPublicKey()));

//  submit upvote event to SC
    submitRelayEvent(badgeAwardUpvoteEvent_2, superconductorRelayUrl);
    TimeUnit.MILLISECONDS.sleep(1500);

// aImg_2 sanity check		
    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_B = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(defnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlTwo, aImg_2_EventSubscriber_B);

    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_B, 1, "2");
  }

  private BaseEvent createSearchRelaysListEventMessage() {
    log.debug("\nSearch Relays List sent from aImg IT 5556...");
    return new SearchRelaysListEvent(
       Identity.generateRandomIdentity(),
       new RelaysTag(superconductorRelayTagUrl),
       "Search Relays List sent from aImg IT 5556");
  }

  protected void submitRelayEvent(BaseEvent event, String url) {
    assertEquals(true, new NostrEventPublisher(url).send(new EventMessage(event)).getFlag());
  }

  private ReqMessage createAfterImageReqMessage(String subscriberId, PublicKey defnCreatorPublicKey, PubKeyTag recipientPubKeyTag) throws JsonProcessingException {
    ReqMessage reqMessageWithStuff = new ReqMessage(
       subscriberId,
       new Filters(
          new KindFilter(
             Kind.BADGE_AWARD_EVENT),
//            new IdentifierTagFilter(reputationIdentifierTag),
          new AddressTagFilter(
             new AddressTag(
                Kind.BADGE_DEFINITION_EVENT,
                defnCreatorPublicKey,
                reputationIdentifierTag,
                afterimageRelayTagUrlTwo)),
          new ReferencedPublicKeyFilter(
             recipientPubKeyTag)));

    ReqMessage reqMessage = reqMessageWithStuff;
    log.debug(Util.prettyFormatJson(reqMessage.encode()));
    return reqMessage;
  }

  protected void submitAfterImageReqWithSubscriber(PublicKey defnCreator, PubKeyTag recipientPubKeyTag, String url, RequestSubscriber<BaseMessage> subscriber) throws JsonProcessingException {
    new NostrSingleRequestService().send(
       createAfterImageReqMessage(
          Factory.generateRandomHex64String(),
          defnCreator,
          recipientPubKeyTag),
       url, subscriber);
  }

  protected List<EventIF> validateSpecificAfterimageRequestResults(RequestSubscriber<BaseMessage> subscriber, int count, String expectedScore) {
    List<EventIF> events =
       validateGeneralAfterimageRequestResults(
          getGenericEvents(subscriber.getItems()));
    assertEquals(count, (long) events.size());
    assertEquals(expectedScore, events.getFirst().getContent());
    return events;
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

  protected List<EventIF> getGenericEvents(List<BaseMessage> messages) {
    return messages.stream()
       .filter(EventMessage.class::isInstance)
       .map(EventMessage.class::cast)
       .map(EventMessage::getEvent)
       .toList();
  }
}
