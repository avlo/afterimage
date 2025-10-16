package com.prosilion.afterimage;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.BadgeAwardDownvoteEvent;
import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.nostr.event.AbstractBadgeAwardEvent;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.ReferenceTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static com.prosilion.afterimage.config.AfterimageBaseConfig.EXTERNAL_IDENTITY_TAG_DOWNVOTE;
import static com.prosilion.afterimage.config.AfterimageBaseConfig.EXTERNAL_IDENTITY_TAG_UPVOTE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ActiveProfiles("test")
public class ExpressionTest {
  private final Identity afterimageInstanceIdentity = Identity.generateRandomIdentity();
  private final String afterimageRelayUrl = "ws://localhost:5555";
  private final BadgeDefinitionEvent reputationDefinitioniEvent;
  private final List<AbstractBadgeAwardEvent<?>> voteEvents = new ArrayList<>();
  private final Identity authorIdentity = Identity.generateRandomIdentity();

  public ExpressionTest() {
    this.reputationDefinitioniEvent = new BadgeDefinitionEvent(
        afterimageInstanceIdentity,
        new IdentifierTag(
            AfterimageKindType.UNIT_REPUTATION.getName()),
        new ReferenceTag(
            afterimageRelayUrl),
        List.of(
            EXTERNAL_IDENTITY_TAG_UPVOTE,
            EXTERNAL_IDENTITY_TAG_DOWNVOTE),
        "afterimage reputation definition f(x)");

    this.voteEvents.add(
        new BadgeAwardUpvoteEvent(
            authorIdentity,
            Identity.generateRandomIdentity().getPublicKey(),
            new BadgeDefinitionEvent(
                authorIdentity,
                new IdentifierTag(SuperconductorKindType.UNIT_UPVOTE.getName()),
                new ReferenceTag(afterimageRelayUrl),
                "1")));

    this.voteEvents.add(
        new BadgeAwardDownvoteEvent(
            authorIdentity,
            Identity.generateRandomIdentity().getPublicKey(),
            new BadgeDefinitionEvent(
                authorIdentity,
                new IdentifierTag(SuperconductorKindType.UNIT_DOWNVOTE.getName()),
                new ReferenceTag(afterimageRelayUrl),
                "-1")));
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
  void testVariableParserWithOperatorWithValuesFromEvents() {
    BigDecimal startingTotalIsZero = BigDecimal.ZERO;
    String CURRENT_TOTAL_STRING = "CURRENT_TOTAL";

    List<ExternalIdentityTag> typeSpecificTagsStream =
        Filterable.getTypeSpecificTags(ExternalIdentityTag.class, reputationDefinitioniEvent);

    Map<String, String> bothMap =
        typeSpecificTagsStream.stream().collect(
            Collectors
                .toMap(
                    externalTag -> externalTag.getIdentifierTag().getUuid(),
                    externalTag -> externalTag.getFormula(),
                    (prev, next) -> next, HashMap::new));


    List<String> operandsWithOperator = voteEvents.stream().map(abstractBadgeAwardEvent ->
    {
      String kindType = abstractBadgeAwardEvent.getKindType().toString();
      String name = kindType.toUpperCase();
      return bothMap.get(name);
    }).toList();


    String reduce = operandsWithOperator
        .stream()
        .reduce("0", (subtotal, element) ->
            doCalc(CURRENT_TOTAL_STRING, subtotal, element));


    assertEquals("0", reduce);
  }

  @SneakyThrows
  private String doCalc(String currentTotalString, String currentTotal, String operator) {
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

    String UNIT_UPVOTE_STRING = SuperconductorKindType.UNIT_UPVOTE.getName();
    Number UNIT_UPVOTE_VALUE = parsePlusSign("+1");

    BigDecimal resultAfterUpvote = new Expression(
        String.format("%s + %s", CURRENT_TOTAL_STRING, UNIT_UPVOTE_STRING))
        .with(CURRENT_TOTAL_STRING, startingTotalIsZero)
        .and(UNIT_UPVOTE_STRING, UNIT_UPVOTE_VALUE)
        .evaluate().getNumberValue();
    assertEquals(new BigDecimal("1"), resultAfterUpvote);

    String UNIT_DOWNVOTE_STRING = SuperconductorKindType.UNIT_DOWNVOTE.getName();
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
