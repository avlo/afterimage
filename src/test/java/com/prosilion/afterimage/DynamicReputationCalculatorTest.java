package com.prosilion.afterimage;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.calculator.DynamicReputationCalculator;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.service.reactive.AbstractIT;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static com.prosilion.afterimage.service.reactive.AbstractIT.AWARD_UNIT_UPVOTE;
import static com.prosilion.afterimage.service.reactive.AbstractIT.FORMULA_UNIT_UPVOTE;
import static com.prosilion.afterimage.service.reactive.AbstractIT.MINUS_ONE_FORMULA;
import static com.prosilion.afterimage.service.reactive.AbstractIT.PLUS_ONE_FORMULA;
import static com.prosilion.afterimage.service.reactive.AbstractIT.downvoteIdentifierTag;
import static com.prosilion.afterimage.service.reactive.AbstractIT.formulaCreator;
import static com.prosilion.afterimage.service.reactive.AbstractIT.formulaDownvoteIdentifierTag;
import static com.prosilion.afterimage.service.reactive.AbstractIT.formulaUpvoteIdentifierTag;
import static com.prosilion.afterimage.service.reactive.AbstractIT.recipient;
import static com.prosilion.afterimage.service.reactive.AbstractIT.repDefnCreator;
import static com.prosilion.afterimage.service.reactive.AbstractIT.reputationIdentifierTag;
import static com.prosilion.afterimage.service.reactive.AbstractIT.submitter;
import static com.prosilion.afterimage.service.reactive.AbstractIT.upvoteDefnCreator;
import static com.prosilion.afterimage.service.reactive.AbstractIT.upvoteIdentifierTag;
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

  public DynamicReputationCalculatorTest() throws ParseException {
    this.dynamicReputationCalculator = new DynamicReputationCalculator(relay.getUrl(), this.afterimageInstanceIdentity);

    BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent = new BadgeDefinitionGenericEvent(upvoteDefnCreator, upvoteIdentifierTag, relay);
    BadgeDefinitionGenericEvent awardDownvoteDefinitionEvent = new BadgeDefinitionGenericEvent(upvoteDefnCreator, downvoteIdentifierTag, relay);

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
       submitter.getPublicKey(),
       reputationIdentifierTag,
       relay,
       AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       plusOneFormulaEvent, minusOneFormulaEvent);

    this.emptyNoReputationYetBadgeAwardEvent = new BadgeAwardReputationEvent(
       afterimageInstanceIdentity,
       recipient.getPublicKey(),
       relay,
       BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG,
       badgeDefinitionReputationContainingPlusOneFormulaEventAndMinusOneFormulaEvent,
       new BigDecimal("0"));
  }

  @Test
  void testCalculatorOnePlusOne() {
    BadgeAwardReputationEvent badgeAwardReputationEvent = dynamicReputationCalculator.calculateUpdatedReputationEvent(
       recipient.getPublicKey(),
       emptyNoReputationYetBadgeAwardEvent,
       List.of(
          plusOneFormulaEvent,
          minusOneFormulaEvent),
       new FollowSetsEvent(
          afterimageInstanceIdentity,
          badgeDefinitionReputationContainingPlusOneFormulaEventAndMinusOneFormulaEvent,
          relay,
          List.of(
             createBadgeAwardEvent(upvoteIdentifierTag),
             createBadgeAwardEvent(downvoteIdentifierTag),
             createBadgeAwardEvent(upvoteIdentifierTag),
             createBadgeAwardEvent(upvoteIdentifierTag))));

    assertEquals("2", badgeAwardReputationEvent.getContent());
  }

  @Test
  void testCalculatorOneMinusOne() throws ParseException {
    String VARIANT = "_VARIANT";
    IdentifierTag formulaUpvoteIdentifierTagVariant = new IdentifierTag(FORMULA_UNIT_UPVOTE + VARIANT);
    IdentifierTag badgeDefnIdentifierTagVariant = new IdentifierTag(AWARD_UNIT_UPVOTE + VARIANT);

    FormulaEvent secondFormulaShouldNotInterfereWithFirstFormula = new FormulaEvent(
       formulaCreator,
       formulaUpvoteIdentifierTagVariant,
       relay,
       new BadgeDefinitionGenericEvent(upvoteDefnCreator, badgeDefnIdentifierTagVariant, relay),
       PLUS_ONE_FORMULA);

    FollowSetsEvent followSetsSingleUpvote = new FollowSetsEvent(
       afterimageInstanceIdentity,
       badgeDefinitionReputationContainingPlusOneFormulaEventAndMinusOneFormulaEvent,
       relay,
       createBadgeAwardEvent(upvoteIdentifierTag));
    
    BadgeAwardReputationEvent badgeAwardReputationEvent = dynamicReputationCalculator.calculateUpdatedReputationEvent(
       recipient.getPublicKey(),
       emptyNoReputationYetBadgeAwardEvent,
       List.of(
          plusOneFormulaEvent,
          secondFormulaShouldNotInterfereWithFirstFormula),
       followSetsSingleUpvote);

    assertEquals("1", badgeAwardReputationEvent.getContent());
  }

  @Test
  void testCalculatorOnePlusTen() throws ParseException {
    String AWARD_10_UPVOTE = "TEST_10_UPVOTE";
    String FORMULA_10_UPVOTE = "FORMULA_TEN_UPVOTE";
    IdentifierTag formulaIdentifierTag = new IdentifierTag(FORMULA_10_UPVOTE);
    IdentifierTag reputationDefinitionIdentifierTag = new IdentifierTag(AWARD_10_UPVOTE);
    FormulaEvent plusTenFormulaEvent = new FormulaEvent(
       formulaCreator,
       formulaIdentifierTag,
       relay,
       new BadgeDefinitionGenericEvent(upvoteDefnCreator, reputationDefinitionIdentifierTag, relay),
       "+10");

    BadgeDefinitionReputationEvent badgeDefinitionReputationEventAddTen = new BadgeDefinitionReputationEvent(
       repDefnCreator,
       submitter.getPublicKey(),
       AbstractIT.reputationIdentifierTag,
       relay,
       BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       plusTenFormulaEvent);

    BadgeAwardReputationEvent badgeAwardNoRepYet = new BadgeAwardReputationEvent(
       afterimageInstanceIdentity,
       recipient.getPublicKey(),
       relay,
       BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG,
       badgeDefinitionReputationEventAddTen,
       new BigDecimal("0"));

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardPlus10Event = createBadgeAwardEvent(reputationDefinitionIdentifierTag);
    FollowSetsEvent incomingFollowSetsEvent = new FollowSetsEvent(
       afterimageInstanceIdentity,
       badgeDefinitionReputationEventAddTen,
       relay,
       badgeAwardPlus10Event);

    BadgeAwardReputationEvent badgeAwardReputationEvent = dynamicReputationCalculator.calculateUpdatedReputationEvent(
       recipient.getPublicKey(),
       badgeAwardNoRepYet,
       List.of(
          plusTenFormulaEvent),
       incomingFollowSetsEvent);

    assertEquals("10", badgeAwardReputationEvent.getContent());
  }

  private BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> createBadgeAwardEvent(IdentifierTag identifierTag) {
    return new BadgeAwardGenericEvent<>(
       submitter, recipient.getPublicKey(), relay,
       new BadgeDefinitionGenericEvent(
          upvoteDefnCreator,
          identifierTag,
          relay,
          String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteDefnCreator.getPublicKey())));
  }
}
