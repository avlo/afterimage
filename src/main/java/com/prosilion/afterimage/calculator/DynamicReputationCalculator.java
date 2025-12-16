package com.prosilion.afterimage.calculator;

import com.ezylang.evalex.Expression;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.springframework.lang.NonNull;

public class DynamicReputationCalculator implements ReputationCalculatorIF {
  private final Identity aImgIdentity;

  public DynamicReputationCalculator(@NonNull Identity aImgIdentity) {
    this.aImgIdentity = aImgIdentity;
  }

  public EventIF calculateUpdatedReputationEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull BadgeAwardReputationEvent previousReputationEvent,
      @NonNull List<FormulaEvent> formulaEvents,
      @NonNull EventIF incomingFollowSetsEvent) throws NostrException {

    List<IdentifierTag> eventVoteTags = incomingFollowSetsEvent.getTags().stream()
        .filter(AddressTag.class::isInstance)
        .map(AddressTag.class::cast)
        .map(AddressTag::getIdentifierTag)
        .filter(Objects::nonNull)
        .filter(
            formulaEvents.stream()
                .map(FormulaEvent::getBadgeDefinitionAwardEvent)
                .map(BadgeDefinitionAwardEvent::getIdentifierTag).toList()::contains)
        .toList();

    String updatedScore = calculateReputationEventScore(
        eventVoteTags,
        previousReputationEvent,
        formulaEvents);

    EventIF reputationEvent = createReputationEvent(
        voteReceiverPubkey,
        updatedScore,
        previousReputationEvent.getBadgeDefinitionReputationEvent(),
        formulaEvents);

    return reputationEvent;
  }

  private String calculateReputationEventScore(
      List<IdentifierTag> voteEvents,
      BadgeAwardReputationEvent previousReputationEvent,
      List<FormulaEvent> formulaEvents) {
    String result =
        voteEvents.stream().map(voteEventType ->
                formulaEvents.stream()
                    .filter(formulaEvent ->
                        formulaEvent.getBadgeDefinitionAwardEvent().getIdentifierTag().equals(voteEventType)).collect(
                        Collectors.toMap(
                            formulaEvent ->
                                formulaEvent.getBadgeDefinitionAwardEvent().getIdentifierTag().getUuid(),
                            FormulaEvent::getFormula,
                            (prev, next) -> next, HashMap::new)).get(voteEventType.getUuid()))
            .reduce(previousReputationEvent.getContent(), this::doCalc);
    return result;
  }

  @SneakyThrows
  private String doCalc(String currentTotal, String operation) {
    final String currentTotalString = "total";
    BigDecimal result = new Expression(
        String.format("%s %s", currentTotalString, operation))
        .with(currentTotalString, new BigDecimal(currentTotal))
        .evaluate().getNumberValue();
    return result.toString();
  }

  private EventIF createReputationEvent(
      @NonNull PublicKey badgeReceiverPubkey,
      @NonNull String score,
      @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent,
      @NonNull List<FormulaEvent> formulaEvents) throws NostrException {
    BadgeAwardReputationEvent badgeAwardReputationEvent = new BadgeAwardReputationEvent(
        aImgIdentity,
        badgeReceiverPubkey,
        badgeDefinitionReputationEvent,
        new BigDecimal(score));
    return badgeAwardReputationEvent;
  }

  @Override
  public String getFullyQualifiedCalculatorName() {
    return getClass().getName();
  }
}
