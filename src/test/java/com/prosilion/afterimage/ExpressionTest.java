package com.prosilion.afterimage;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static com.prosilion.afterimage.config.AfterimageBaseConfig.IDENTITY;
import static com.prosilion.afterimage.config.AfterimageBaseConfig.MINUS_ONE_FORMULA;
import static com.prosilion.afterimage.config.AfterimageBaseConfig.PLATFORM;
import static com.prosilion.afterimage.config.AfterimageBaseConfig.PLUS_ONE_FORMULA;
import static com.prosilion.afterimage.config.AfterimageBaseConfig.PROOF;
import static com.prosilion.afterimage.config.AfterimageBaseConfig.UNIT_DOWNVOTE;
import static com.prosilion.afterimage.config.AfterimageBaseConfig.UNIT_UPVOTE;
import static com.prosilion.afterimage.enums.AfterimageKindType.UNIT_REPUTATION;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ActiveProfiles("test")
public class ExpressionTest {
  public static final IdentifierTag upvoteIdentifierTag = new IdentifierTag(UNIT_UPVOTE);
  public static final IdentifierTag downvoteIdentifierTag = new IdentifierTag(UNIT_DOWNVOTE);
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventAddOneSubtractOne;
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventAddOneAddOne;
  private final ExternalIdentityTag externalIdentityTag = new ExternalIdentityTag(PLATFORM, IDENTITY, PROOF);
  private final Relay relay = new Relay("ws://localhost:5555");

  public ExpressionTest() throws ParseException {
    Identity afterimageInstanceIdentity = Identity.generateRandomIdentity();

    BadgeDefinitionAwardEvent upvoteDefinitionEvent = new BadgeDefinitionAwardEvent(afterimageInstanceIdentity, upvoteIdentifierTag, relay);
    BadgeDefinitionAwardEvent downvoteDefinitionEvent = new BadgeDefinitionAwardEvent(afterimageInstanceIdentity, downvoteIdentifierTag, relay);
    this.badgeDefinitionReputationEventAddOneSubtractOne = new BadgeDefinitionReputationEvent(
        afterimageInstanceIdentity,
        new IdentifierTag(
            UNIT_REPUTATION.getName()),
        relay,
        externalIdentityTag,
        List.of(
            new FormulaEvent(
                afterimageInstanceIdentity,
                upvoteDefinitionEvent,
                PLUS_ONE_FORMULA),
            new FormulaEvent(
                afterimageInstanceIdentity,
                downvoteDefinitionEvent,
                MINUS_ONE_FORMULA)));

    this.badgeDefinitionReputationEventAddOneAddOne = new BadgeDefinitionReputationEvent(
        afterimageInstanceIdentity,
        new IdentifierTag(
            UNIT_REPUTATION.getName()),
        relay,
        externalIdentityTag,
        List.of(
            new FormulaEvent(
                afterimageInstanceIdentity,
                upvoteDefinitionEvent,
                PLUS_ONE_FORMULA),
            new FormulaEvent(
                afterimageInstanceIdentity,
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
