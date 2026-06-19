package com.prosilion.afterimage;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.calculator.ExpressionCalculator;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.user.Identity;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static com.prosilion.afterimage.service.reactive.AbstractIT.MINUS_ONE_FORMULA;
import static com.prosilion.afterimage.service.reactive.AbstractIT.PLUS_ONE_FORMULA;
import static com.prosilion.afterimage.service.reactive.AbstractIT.downvoteIdentifierTag;
import static com.prosilion.afterimage.service.reactive.AbstractIT.formulaDownvoteIdentifierTag;
import static com.prosilion.afterimage.service.reactive.AbstractIT.formulaUpvoteIdentifierTag;
import static com.prosilion.afterimage.service.reactive.AbstractIT.reputationIdentifierTag;
import static com.prosilion.afterimage.service.reactive.AbstractIT.upvoteDefnCreator;
import static com.prosilion.afterimage.service.reactive.AbstractIT.upvoteIdentifierTag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@ActiveProfiles("test")
public class ExpressionCalculatorTest {

  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventAddOneSubtractOne;
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventAddOneAddOne;
  private final Relay relay = new Relay("ws://localhost:5555");

  public ExpressionCalculatorTest() throws ParseException {
    Identity afterimageInstanceIdentity = Identity.generateRandomIdentity();

    BadgeDefinitionGenericEvent upvoteDefinitionEvent = new BadgeDefinitionGenericEvent(
       upvoteDefnCreator,
       upvoteIdentifierTag,
       relay,
       String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteDefnCreator.getPublicKey()));

    BadgeDefinitionGenericEvent downvoteDefinitionEvent = new BadgeDefinitionGenericEvent(upvoteDefnCreator, downvoteIdentifierTag, relay);

    this.badgeDefinitionReputationEventAddOneSubtractOne = new BadgeDefinitionReputationEvent(
       afterimageInstanceIdentity,
       afterimageInstanceIdentity.getPublicKey(),
       reputationIdentifierTag,
       relay,
       BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       List.of(
          new FormulaEvent(
             afterimageInstanceIdentity,
             formulaUpvoteIdentifierTag,
             relay,
             upvoteDefinitionEvent,
             PLUS_ONE_FORMULA),
          new FormulaEvent(
             afterimageInstanceIdentity,
             formulaDownvoteIdentifierTag,
             relay,
             downvoteDefinitionEvent,
             MINUS_ONE_FORMULA)));

    this.badgeDefinitionReputationEventAddOneAddOne = new BadgeDefinitionReputationEvent(
       afterimageInstanceIdentity,
       afterimageInstanceIdentity.getPublicKey(),
       reputationIdentifierTag,
       relay,
       BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       List.of(
          new FormulaEvent(
             afterimageInstanceIdentity,
             formulaUpvoteIdentifierTag,
             relay,
             upvoteDefinitionEvent,
             PLUS_ONE_FORMULA),
          new FormulaEvent(
             afterimageInstanceIdentity,
             formulaDownvoteIdentifierTag,
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
          .reduce(ExpressionCalculator::calculate)
          .orElseThrow());
  }

  @Test
  void testAddOneAddOne() {
    log.info(badgeDefinitionReputationEventAddOneAddOne.getContent());
    assertEquals(
       "2",
       badgeDefinitionReputationEventAddOneAddOne.getFormulaEvents().stream()
          .map(FormulaEvent::getFormula)
          .reduce(ExpressionCalculator::calculate)
          .orElseThrow());
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

  @Test
  void testVariousOperations() {
    testVariousSpaceTabFormatHandling("1", "0", "+", "1");
    testVariousSpaceTabFormatHandling("-1", "0", "-", "1");

    testVariousSpaceTabFormatHandling("10", "0", "+", "10");
    testVariousSpaceTabFormatHandling("10", "10", "+", "0");
    testVariousSpaceTabFormatHandling("10", "10", "-", "0");

    testVariousSpaceTabFormatHandling("-1", "0", "+", "-1");

    testVariousSpaceTabFormatHandling("-10", "0", "+", "-10");
    testVariousSpaceTabFormatHandling("-20", "-10", "+", "-10");
  }

  void testVariousSpaceTabFormatHandling(String expected, String currentTotal, String operator, String operand) {
    assertEquals(expected, ExpressionCalculator.calculate(currentTotal, operator + operand));
    assertEquals(expected, ExpressionCalculator.calculate(currentTotal, " " + operator + operand));
    assertEquals(expected, ExpressionCalculator.calculate(currentTotal, "   " + operator + operand));
    assertEquals(expected, ExpressionCalculator.calculate(currentTotal, "   " + operator + operand));
    assertEquals(expected, ExpressionCalculator.calculate(currentTotal, "   " + operator + "   " + operand));
    assertEquals(expected, ExpressionCalculator.calculate(currentTotal, "   " + operator + "   " + operand + "   "));
    assertEquals(expected, ExpressionCalculator.calculate(" " + currentTotal, operator + operand));
    assertEquals(expected, ExpressionCalculator.calculate("    " + currentTotal, operator + operand));
    assertEquals(expected, ExpressionCalculator.calculate(" " + currentTotal + " ", operator + operand));
    assertEquals(expected, ExpressionCalculator.calculate("  " + currentTotal + "   ", operator + operand));
  }

  @Test
  void testExceptions() {
    assertThrows(NumberFormatException.class, () -> ExpressionCalculator.calculate("+-10", " +  - 10"));
    assertThrows(NostrException.class, () -> ExpressionCalculator.calculate("10", "0"));
  }

  private static Number parsePlusSign(String operand) throws java.text.ParseException {
    String parsedOperand = operand.startsWith("+") ? operand.substring(1) : operand;
    return NumberFormat.getInstance().parse(parsedOperand);
  }
}
