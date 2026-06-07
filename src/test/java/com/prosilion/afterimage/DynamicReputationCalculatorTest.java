package com.prosilion.afterimage;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.calculator.DynamicReputationCalculator;
import com.prosilion.afterimage.enums.AfterimageKindType;
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

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ActiveProfiles("test")
public class DynamicReputationCalculatorTest {
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
  protected final IdentifierTag formulaPlusOneIdentifierTag = new IdentifierTag(FORMULA_UNIT_UPVOTE);
  protected final IdentifierTag formulaMinusOneIdentifierTag = new IdentifierTag(FORMULA_UNIT_DOWNVOTE);

  private final Identity aImgIdentity;
  private final Identity defnCreator = Identity.generateRandomIdentity();
  private final Identity submitter = Identity.generateRandomIdentity();
  private final Identity recipient = Identity.generateRandomIdentity();

  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventAddOneSubtractOne;
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventAddOneAddOne;
  private final Relay relay = new Relay("ws://localhost:5555");

  private final FormulaEvent plusOneFormulaEvent;
  private final FormulaEvent minusOneFormulaEvent;
  private final BadgeAwardReputationEvent emptyNoReputationYetBadgeAwardEvent;
  private final DynamicReputationCalculator dynamicReputationCalculator;

  public DynamicReputationCalculatorTest() throws ParseException {
    this.aImgIdentity = Identity.generateRandomIdentity();
    this.dynamicReputationCalculator = new DynamicReputationCalculator(relay.getUrl(), aImgIdentity);

    BadgeDefinitionGenericEvent upvoteDefinitionEvent = new BadgeDefinitionGenericEvent(aImgIdentity, upvoteIdentifierTag, relay);
    BadgeDefinitionGenericEvent downvoteDefinitionEvent = new BadgeDefinitionGenericEvent(aImgIdentity, downvoteIdentifierTag, relay);

    this.plusOneFormulaEvent = new FormulaEvent(
        aImgIdentity,
        formulaPlusOneIdentifierTag,
        relay,
        upvoteDefinitionEvent,
        PLUS_ONE_FORMULA);
    this.minusOneFormulaEvent = new FormulaEvent(
        aImgIdentity,
        formulaMinusOneIdentifierTag,
        relay,
        downvoteDefinitionEvent,
        MINUS_ONE_FORMULA);

    this.badgeDefinitionReputationEventAddOneSubtractOne = new BadgeDefinitionReputationEvent(
        aImgIdentity,
        aImgIdentity.getPublicKey(),
        reputationIdentifierTag,
        relay,
        BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
        List.of(
            plusOneFormulaEvent,
            minusOneFormulaEvent));

    this.badgeDefinitionReputationEventAddOneAddOne = new BadgeDefinitionReputationEvent(
        aImgIdentity,
        aImgIdentity.getPublicKey(),
        reputationIdentifierTag,
        relay,
        BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
        List.of(
            plusOneFormulaEvent,
            new FormulaEvent(
                aImgIdentity,
                formulaMinusOneIdentifierTag, // convenient repurpose of minusOne tag to add another +1 event content
                relay,
                downvoteDefinitionEvent,
                PLUS_ONE_FORMULA)));

    this.emptyNoReputationYetBadgeAwardEvent = new BadgeAwardReputationEvent(
        aImgIdentity,
        recipient.getPublicKey(),
        relay,
        AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG,
        badgeDefinitionReputationEventAddOneAddOne,
        new BigDecimal("0"));
  }

  @Test
  void testCalculatorOnePlusOne() throws ParseException {
    BadgeAwardReputationEvent badgeAwardReputationEvent = dynamicReputationCalculator.calculateUpdatedReputationEvent(
        recipient.getPublicKey(),
        emptyNoReputationYetBadgeAwardEvent,
        List.of(
            plusOneFormulaEvent,
            new FormulaEvent(
                aImgIdentity,
                new IdentifierTag(FORMULA_UNIT_UPVOTE + "_AGAIN"),
                relay,
                new BadgeDefinitionGenericEvent(aImgIdentity, new IdentifierTag(AWARD_UNIT_UPVOTE + "_AGAIN"), relay),
                PLUS_ONE_FORMULA)),
        new FollowSetsEvent(
            aImgIdentity,
            badgeDefinitionReputationEventAddOneAddOne,
            relay,
            createBadgeAwardEvent(upvoteIdentifierTag)));

    assertEquals("1", badgeAwardReputationEvent.getContent());
  }

  @Test
  void testCalculatorOnePlusTen() throws ParseException {
    String AWARD_TEN_UPVOTE = "AWARD_TEN_UPVOTE";
    IdentifierTag upvoteTenIdentifierTag = new IdentifierTag(AWARD_TEN_UPVOTE);

    String FORMULA_10_UPVOTE = "FORMULA_10_UPVOTE";
    IdentifierTag formulaPlusTenIdentifierTag = new IdentifierTag(FORMULA_10_UPVOTE);

    FormulaEvent plusTenFormulaEvent = new FormulaEvent(
        aImgIdentity,
        formulaPlusTenIdentifierTag,
        relay,
        new BadgeDefinitionGenericEvent(aImgIdentity, upvoteTenIdentifierTag, relay),
        "+10");

    BadgeDefinitionReputationEvent badgeDefinitionReputationEventAddTen = new BadgeDefinitionReputationEvent(
        aImgIdentity,
        aImgIdentity.getPublicKey(),
        reputationIdentifierTag,
        relay,
        BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
        plusTenFormulaEvent);

    BadgeAwardReputationEvent badgeAwardNoRepYet = new BadgeAwardReputationEvent(
        aImgIdentity,
        recipient.getPublicKey(),
        relay,
        AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG,
        badgeDefinitionReputationEventAddTen,
        new BigDecimal("0"));

    BadgeAwardReputationEvent badgeAwardReputationEvent = dynamicReputationCalculator.calculateUpdatedReputationEvent(
        recipient.getPublicKey(),
        badgeAwardNoRepYet,
        List.of(
            plusTenFormulaEvent),
        new FollowSetsEvent(
            aImgIdentity,
            badgeDefinitionReputationEventAddTen,
            relay,
            createBadgeAwardEvent(formulaPlusTenIdentifierTag)));

    assertEquals("10", badgeAwardReputationEvent.getContent());
  }

  private BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> createBadgeAwardEvent(IdentifierTag identifierTag) {
    return new BadgeAwardGenericEvent<>(
        submitter, recipient.getPublicKey(), relay,
        new BadgeDefinitionGenericEvent(
            defnCreator,
            identifierTag,
            relay,
            String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", defnCreator.getPublicKey())));
  }
}
