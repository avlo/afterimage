package com.prosilion.afterimage;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.calculator.DynamicReputationCalculator;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BadgeSetsEvent;
import com.prosilion.nostr.event.CuratedBadgeAwardGenericEvent;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.ReferenceTag;
import com.prosilion.nostr.tag.SetsPairedEvent;
import com.prosilion.nostr.user.Identity;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static com.prosilion.afterimage.service.reactive.AbstractIT.AWARD_UNIT_UPVOTE;
import static com.prosilion.afterimage.service.reactive.AbstractIT.FORMULA_UNIT_UPVOTE;
import static com.prosilion.afterimage.service.reactive.AbstractIT.MINUS_ONE_FORMULA;
import static com.prosilion.afterimage.service.reactive.AbstractIT.PLUS_ONE_FORMULA;
import static com.prosilion.afterimage.service.reactive.AbstractIT.formulaCreator;
import static com.prosilion.afterimage.service.reactive.AbstractIT.formulaDownvoteIdentifierTag;
import static com.prosilion.afterimage.service.reactive.AbstractIT.formulaUpvoteIdentifierTag;
import static com.prosilion.afterimage.service.reactive.AbstractIT.recipient;
import static com.prosilion.afterimage.service.reactive.AbstractIT.repDefnCreator;
import static com.prosilion.afterimage.service.reactive.AbstractIT.reputationIdentifierTag;
import static com.prosilion.afterimage.service.reactive.AbstractIT.submitter;
import static com.prosilion.afterimage.service.reactive.AbstractIT.upvoteAndOrDownvoteDefnCreator;
import static com.prosilion.nostr.tag.SetsPairedEvent.NULL_EVENT_TAG_RELAY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ActiveProfiles("test")
public class DynamicReputationCalculatorTest {
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationContainingPlusOneFormulaEventAndMinusOneFormulaEvent;

  private final Relay relay = new Relay("ws://localhost:5555");

  private final FormulaEvent plusOneFormulaEvent;
  private final FormulaEvent minusOneFormulaEvent;
  private final BadgeAwardReputationEvent emptyNoReputationYetBadgeAwardEvent;
  private final DynamicReputationCalculator dynamicReputationCalculator;

  protected final Identity afterimageInstanceIdentity = Identity.generateRandomIdentity();
//     Identity.create("2684585483196998204846989544737603523651520600328805626488477202"); // aImg-test private key

  BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent;
  BadgeDefinitionGenericEvent awardDownvoteDefinitionEvent;

  public DynamicReputationCalculatorTest() throws ParseException {
    this.dynamicReputationCalculator = new DynamicReputationCalculator(relay.getUrl(), this.afterimageInstanceIdentity);

    this.awardUpvoteDefinitionEvent = new BadgeDefinitionGenericEvent(upvoteAndOrDownvoteDefnCreator, formulaUpvoteIdentifierTag, relay);
    this.awardDownvoteDefinitionEvent = new BadgeDefinitionGenericEvent(upvoteAndOrDownvoteDefnCreator, formulaDownvoteIdentifierTag, relay);

    this.plusOneFormulaEvent = new FormulaEvent(
       formulaCreator,
       formulaUpvoteIdentifierTag,
       relay,
       awardUpvoteDefinitionEvent,
       PLUS_ONE_FORMULA);

    this.minusOneFormulaEvent = new FormulaEvent(
       formulaCreator,
       formulaDownvoteIdentifierTag,
       relay,
       awardDownvoteDefinitionEvent,
       MINUS_ONE_FORMULA);

    this.badgeDefinitionReputationContainingPlusOneFormulaEventAndMinusOneFormulaEvent = new BadgeDefinitionReputationEvent(
       repDefnCreator,
       afterimageInstanceIdentity.getPublicKey(),
       reputationIdentifierTag,
       relay,
       AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       plusOneFormulaEvent, minusOneFormulaEvent);

    this.emptyNoReputationYetBadgeAwardEvent = new BadgeAwardReputationEvent(
       afterimageInstanceIdentity,
       recipient.getPublicKey(),
       BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG,
       badgeDefinitionReputationContainingPlusOneFormulaEventAndMinusOneFormulaEvent,
       new BigDecimal("0"),
       relay);
  }

  public static SetsPairedEvent create(BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> event, Relay backupRelay) {
    return new SetsPairedEvent(
       event.getBadgeDefinitionEvent().asAddressableEventAddressTag(),
       new EventTag(
          event.getId(),
          event.getRelay().map(Relay::getUrl).orElseThrow(() ->
             new NostrException(NULL_EVENT_TAG_RELAY))));
  }

  private BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> createBadgeAwardEvent(BadgeDefinitionGenericEvent awardDefinitionEvent) {
    return new BadgeAwardGenericEvent<>(
       submitter,
       recipient.getPublicKey(),
       awardDefinitionEvent,
       String.format("awardDefinitionEvent, definition creator PublicKey: [%s]", upvoteAndOrDownvoteDefnCreator.getPublicKey()),
       relay);
  }

  @Test
  void testCalculatorOnePlusOne() {
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardEvent = createBadgeAwardEvent(awardUpvoteDefinitionEvent);
    CuratedBadgeAwardGenericEvent curationSetsEvent = new CuratedBadgeAwardGenericEvent(
       afterimageInstanceIdentity,
       badgeAwardEvent,
       new ReferenceTag(awardDownvoteDefinitionEvent.getRelay().orElseThrow().getUrl()),
       new ReferenceTag(badgeAwardEvent.getRelay().orElseThrow().getUrl()),
       relay);

    BadgeSetsEvent badgeSetsEvent = new BadgeSetsEvent(
       afterimageInstanceIdentity,
       badgeDefinitionReputationContainingPlusOneFormulaEventAndMinusOneFormulaEvent,
       curationSetsEvent,
       relay);

    BadgeAwardReputationEvent badgeAwardReputationEvent = dynamicReputationCalculator.calculateUpdatedReputationEvent(
       recipient.getPublicKey(),
       emptyNoReputationYetBadgeAwardEvent,
       List.of(plusOneFormulaEvent, minusOneFormulaEvent),
       new FollowSetsEvent(
          afterimageInstanceIdentity,
          badgeSetsEvent, relay));

    assertEquals("1", badgeAwardReputationEvent.getContent());
  }

  @Test
  void testCalculatorOneMinusOne() {
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardEvent = createBadgeAwardEvent(awardDownvoteDefinitionEvent);
    CuratedBadgeAwardGenericEvent curationSetsEvent = new CuratedBadgeAwardGenericEvent(
       afterimageInstanceIdentity,
       badgeAwardEvent,
       new ReferenceTag(awardDownvoteDefinitionEvent.getRelay().orElseThrow().getUrl()),
       new ReferenceTag(badgeAwardEvent.getRelay().orElseThrow().getUrl()),
       relay);

    BadgeSetsEvent badgeSetsEvent = new BadgeSetsEvent(
       afterimageInstanceIdentity,
       badgeDefinitionReputationContainingPlusOneFormulaEventAndMinusOneFormulaEvent,
       curationSetsEvent,
       relay);

    BadgeAwardReputationEvent badgeAwardReputationEvent = dynamicReputationCalculator.calculateUpdatedReputationEvent(
       recipient.getPublicKey(),
       emptyNoReputationYetBadgeAwardEvent,
       List.of(plusOneFormulaEvent, minusOneFormulaEvent),
       new FollowSetsEvent(
          afterimageInstanceIdentity,
          badgeSetsEvent, relay));

    assertEquals("-1", badgeAwardReputationEvent.getContent());
  }

  @Test
  void testCalculatorOneMinusOneVariant() throws ParseException {
    String VARIANT = "_VARIANT";
    IdentifierTag formulaUpvoteIdentifierTagVariant = new IdentifierTag(FORMULA_UNIT_UPVOTE + VARIANT);
    IdentifierTag badgeDefnIdentifierTagVariant = new IdentifierTag(AWARD_UNIT_UPVOTE + VARIANT);

    FormulaEvent secondFormulaShouldNotInterfereWithFirstFormula = new FormulaEvent(
       formulaCreator,
       formulaUpvoteIdentifierTagVariant,
       relay,
       new BadgeDefinitionGenericEvent(upvoteAndOrDownvoteDefnCreator, badgeDefnIdentifierTagVariant, relay),
       PLUS_ONE_FORMULA);

    BadgeDefinitionReputationEvent localBadgeDefinitionReputationContainingPlusOneFormulaEventAndMinusOneFormulaEvent = new BadgeDefinitionReputationEvent(
       repDefnCreator,
       afterimageInstanceIdentity.getPublicKey(),
       reputationIdentifierTag,
       relay,
       AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       plusOneFormulaEvent, minusOneFormulaEvent);

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent = createBadgeAwardEvent(awardUpvoteDefinitionEvent);
    CuratedBadgeAwardGenericEvent curationSetsEventUpvote = new CuratedBadgeAwardGenericEvent(
       afterimageInstanceIdentity,
       badgeAwardUpvoteEvent,
       new ReferenceTag(awardDownvoteDefinitionEvent.getRelay().orElseThrow().getUrl()),
       new ReferenceTag(badgeAwardUpvoteEvent.getRelay().orElseThrow().getUrl()),
       relay);

    BadgeSetsEvent badgeSetsEvent_1 = new BadgeSetsEvent(
       afterimageInstanceIdentity,
       badgeDefinitionReputationContainingPlusOneFormulaEventAndMinusOneFormulaEvent,
       curationSetsEventUpvote, relay);

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardDownvoteEvent = createBadgeAwardEvent(awardDownvoteDefinitionEvent);
    CuratedBadgeAwardGenericEvent curationSetsEventDownvote = new CuratedBadgeAwardGenericEvent(
       afterimageInstanceIdentity,
       badgeAwardDownvoteEvent,
       new ReferenceTag(awardDownvoteDefinitionEvent.getRelay().orElseThrow().getUrl()),
       new ReferenceTag(badgeAwardUpvoteEvent.getRelay().orElseThrow().getUrl()),
       relay);

    BadgeSetsEvent badgeSetsEvent_2 = new BadgeSetsEvent(
       afterimageInstanceIdentity,
       localBadgeDefinitionReputationContainingPlusOneFormulaEventAndMinusOneFormulaEvent,
       curationSetsEventDownvote, relay);

    BadgeAwardReputationEvent badgeAwardReputationEvent_1 = dynamicReputationCalculator.calculateUpdatedReputationEvent(
       recipient.getPublicKey(),
       emptyNoReputationYetBadgeAwardEvent,
       List.of(
          plusOneFormulaEvent,
          secondFormulaShouldNotInterfereWithFirstFormula),
       new FollowSetsEvent(
          afterimageInstanceIdentity,
          List.of(badgeSetsEvent_1, badgeSetsEvent_1),
          relay));

    assertEquals("1", badgeAwardReputationEvent_1.getContent());
  }

//  @Test
//  void testCalculatorOnePlusTen() throws ParseException {
//    String AWARD_10_UPVOTE = "TEST_10_UPVOTE";
//    String FORMULA_10_UPVOTE = "FORMULA_TEN_UPVOTE";
//    IdentifierTag formulaIdentifierTag = new IdentifierTag(FORMULA_10_UPVOTE);
//    IdentifierTag reputationDefinitionIdentifierTag = new IdentifierTag(AWARD_10_UPVOTE);
//    FormulaEvent plusTenFormulaEvent = new FormulaEvent(
//      formulaCreator,
//      formulaIdentifierTag,
//      relay,
//      new BadgeDefinitionGenericEvent(upvoteDefnCreator, reputationDefinitionIdentifierTag, relay),
//      "+10");
//
//    BadgeDefinitionReputationEvent badgeDefinitionReputationEventAddTen = new BadgeDefinitionReputationEvent(
//      repDefnCreator,
//      submitter.getPublicKey(),
//      AbstractIT.reputationIdentifierTag,
//      relay,
//      BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
//      plusTenFormulaEvent);
//
//    BadgeAwardReputationEvent badgeAwardNoRepYet = new BadgeAwardReputationEvent(
//      afterimageInstanceIdentity,
//      recipient.getPublicKey(),
//      BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG,
//      badgeDefinitionReputationEventAddTen,
//      new BigDecimal("0"),
//      relay);
//
//    BadgeDefinitionGenericEventAux defnAuxNo_defnEvent_NoNo_Upvote = createDefnEventAux(badgeDefinitionReputationEventAddTen, null);
//    SetsPairedEvents badgeSetsUpvoteEventPairedEvents = new SetsPairedEvents(
//      defnAuxNo_defnEvent_NoNo_Upvote,
//      createAwardEventAux(createBadgeAwardEvent(reputationDefinitionIdentifierTag), relay));
//
//    BadgeSetsEvent badgeSetsDownvoteEvent = new BadgeSetsEvent(
//      submitter,
//      badgeDefinitionReputationEventAddTen,
//      badgeSetsUpvoteEventPairedEvents, relay);
//
//    FollowSetsEvent incomingFollowSetsEvent = new FollowSetsEvent(
//      afterimageInstanceIdentity,
//      badgeSetsDownvoteEvent,
//      relay);
//
//    BadgeAwardReputationEvent badgeAwardReputationEvent = dynamicReputationCalculator.calculateUpdatedReputationEvent(
//      recipient.getPublicKey(),
//      Optional.of(badgeAwardNoRepYet),
//      List.of(
//        plusTenFormulaEvent),
//      incomingFollowSetsEvent).get();
//
//    assertEquals("10", badgeAwardReputationEvent.getContent());

  //  }
}
