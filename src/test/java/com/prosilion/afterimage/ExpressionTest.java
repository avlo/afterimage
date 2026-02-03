package com.prosilion.afterimage;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ActiveProfiles("test")
public class ExpressionTest {
  public static final String UNIT_UPVOTE = "UNIT_UPVOTE";
  public static final String UNIT_DOWNVOTE = "UNIT_DOWNVOTE";
  public static final String UNIT_REPUTATION = "UNIT_REPUTATION";
  public static final String PLUS_ONE_FORMULA = "+1";
  public static final String MINUS_ONE_FORMULA = "-1";

  public static final IdentifierTag upvoteIdentifierTag = new IdentifierTag(UNIT_UPVOTE);
  public static final IdentifierTag downvoteIdentifierTag = new IdentifierTag(UNIT_DOWNVOTE);
  public static final IdentifierTag reputationIdentifierTag = new IdentifierTag(UNIT_REPUTATION);
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventAddOneSubtractOne;
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventAddOneAddOne;
  private final Relay relay = new Relay("ws://localhost:5555");

  public static final String FORMULA_PLUS_ONE = "FORMULA_PLUS_ONE";
  private final IdentifierTag formulaPlusOneIdentifierTag = new IdentifierTag(FORMULA_PLUS_ONE);

  public static final String FORMULA_MINUS_ONE = "FORMULA_MINUS_ONE";
  private final IdentifierTag formulaMinusOneIdentifierTag = new IdentifierTag(FORMULA_MINUS_ONE);

  public ExpressionTest() throws ParseException {
    Identity afterimageInstanceIdentity = Identity.generateRandomIdentity();

    BadgeDefinitionGenericEvent upvoteDefinitionEvent = new BadgeDefinitionGenericEvent(afterimageInstanceIdentity, upvoteIdentifierTag, relay);
    BadgeDefinitionGenericEvent downvoteDefinitionEvent = new BadgeDefinitionGenericEvent(afterimageInstanceIdentity, downvoteIdentifierTag, relay);
    this.badgeDefinitionReputationEventAddOneSubtractOne = new BadgeDefinitionReputationEvent(
        afterimageInstanceIdentity,
        reputationIdentifierTag,
        relay,
        BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
        List.of(
            new FormulaEvent(
                afterimageInstanceIdentity,
                formulaPlusOneIdentifierTag,
                relay,
                upvoteDefinitionEvent,
                PLUS_ONE_FORMULA),
            new FormulaEvent(
                afterimageInstanceIdentity,
                formulaMinusOneIdentifierTag,
                relay,
                downvoteDefinitionEvent,
                MINUS_ONE_FORMULA)));

    this.badgeDefinitionReputationEventAddOneAddOne = new BadgeDefinitionReputationEvent(
        afterimageInstanceIdentity,
        reputationIdentifierTag,
        relay,
        BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
        List.of(
            new FormulaEvent(
                afterimageInstanceIdentity,
                formulaPlusOneIdentifierTag,
                relay,
                upvoteDefinitionEvent,
                PLUS_ONE_FORMULA),
            new FormulaEvent(
                afterimageInstanceIdentity,
                formulaMinusOneIdentifierTag,
                relay,
                downvoteDefinitionEvent,
                PLUS_ONE_FORMULA)));
  }

  @Test
  void testHardCodedExpression() throws EvaluationException, ParseException, java.text.ParseException {
    Expression expression = new Expression("(a + b)");
    Number parse = NumberFormat.getInstance().parse("2.5");
    BigDecimal numberValue = expression
        .with("a", 3.5)
        .and("b", parse)
        .evaluate().getNumberValue();

    log.info("numberValue.toString(): {}", numberValue.toString());
    log.info("numberValue.toEngineeringString(): {}", numberValue.toEngineeringString());
    log.info("numberValue.toPlainString(): {}", numberValue.toPlainString());
  }

  @Test
  void testVariableParserWithOperator() throws EvaluationException, ParseException {
    BigDecimal startingTotalIsZero = BigDecimal.ZERO;
    String CURRENT_TOTAL_STRING = "CURRENT_TOTAL";

    String UNIT_UPVOTE_VALUE = "+1";

    BigDecimal resultAfterUpvote = new Expression(
        String.format("%s %s", CURRENT_TOTAL_STRING, UNIT_UPVOTE_VALUE))
        .with(CURRENT_TOTAL_STRING, startingTotalIsZero)
        .evaluate().getNumberValue();
    assertEquals(new BigDecimal("1"), resultAfterUpvote);

    String UNIT_DOWNVOTE_VALUE = "-1";
    assertEquals(
        new BigDecimal("0"),
        new Expression(String.format("%s + %s", CURRENT_TOTAL_STRING, UNIT_DOWNVOTE_VALUE))
            .with(CURRENT_TOTAL_STRING, resultAfterUpvote)
            .evaluate().getNumberValue());
  }

  @Test
  void testAddOneSubtractOne() {
    log.info(badgeDefinitionReputationEventAddOneSubtractOne.getContent());
    assertEquals(
        "0",
        badgeDefinitionReputationEventAddOneSubtractOne.getFormulaEvents().stream()
            .map(FormulaEvent::getFormula)
            .reduce(this::doCalc)
            .orElseThrow());
  }

  @Test
  void testAddOneAddOne() {
    log.info(badgeDefinitionReputationEventAddOneAddOne.getContent());
    assertEquals(
        "2",
        badgeDefinitionReputationEventAddOneAddOne.getFormulaEvents().stream()
            .map(FormulaEvent::getFormula)
            .reduce(this::doCalc)
            .orElseThrow());
  }

  @SneakyThrows
  private String doCalc(String currentTotal, String operator) {
    final String currentTotalString = "total";
    BigDecimal result = new Expression(
        String.format("%s %s", currentTotalString, operator))
        .with(currentTotalString, new BigDecimal(currentTotal))
        .evaluate().getNumberValue();
    return result.toString();
  }

  @Test
  void testVariableParser() throws EvaluationException, ParseException, java.text.ParseException {
    BigDecimal startingTotalIsZero = BigDecimal.ZERO;
    String CURRENT_TOTAL_STRING = "CURRENT_TOTAL";

    String UNIT_UPVOTE_STRING = upvoteIdentifierTag.getUuid();
    Number UNIT_UPVOTE_VALUE = parsePlusSign("+1");

    BigDecimal resultAfterUpvote = new Expression(
        String.format("%s + %s", CURRENT_TOTAL_STRING, UNIT_UPVOTE_STRING))
        .with(CURRENT_TOTAL_STRING, startingTotalIsZero)
        .and(UNIT_UPVOTE_STRING, UNIT_UPVOTE_VALUE)
        .evaluate().getNumberValue();
    assertEquals(new BigDecimal("1"), resultAfterUpvote);

    String UNIT_DOWNVOTE_STRING = downvoteIdentifierTag.getUuid();
    Number UNIT_DOWNVOTE_VALUE = parsePlusSign("-1");
    assertEquals(
        new BigDecimal("0"),
        new Expression(String.format("%s + %s", CURRENT_TOTAL_STRING, UNIT_DOWNVOTE_STRING))
            .with(CURRENT_TOTAL_STRING, resultAfterUpvote)
            .and(UNIT_DOWNVOTE_STRING, UNIT_DOWNVOTE_VALUE)
            .evaluate().getNumberValue());
  }

  private static Number parsePlusSign(String operand) throws java.text.ParseException {
    String parsedOperand = operand.startsWith("+") ? operand.substring(1) : operand;
    return NumberFormat.getInstance().parse(parsedOperand);
  }
}
