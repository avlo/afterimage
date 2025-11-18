package com.prosilion.afterimage.calculator;

import com.ezylang.evalex.Expression;
import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import java.math.BigDecimal;
import java.util.Collection;
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

    List<IdentifierTag> definitionUuids = formulaEvents.stream()
        .map(formulaEvent ->
            Filterable.getTypeSpecificTags(IdentifierTag.class, formulaEvent))
        .flatMap(Collection::stream)
        .toList();

    List<BaseTag> allEventTags = incomingFollowSetsEvent.getTags();

    List<IdentifierTag> eventVoteTags = allEventTags.stream()
        .filter(AddressTag.class::isInstance)
        .map(AddressTag.class::cast)
        .map(AddressTag::getIdentifierTag)
        .filter(Objects::nonNull)
        .filter(definitionUuids::contains)
        .toList();

    String updatedScore = calculateReputationEventScore(eventVoteTags, previousReputationEvent, formulaEvents);

    EventIF reputationEvent = createReputationEvent(voteReceiverPubkey, updatedScore, previousReputationEvent.getBadgeDefinitionReputationEvent(), formulaEvents);
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
                        Filterable.getTypeSpecificTagsStream(IdentifierTag.class, formulaEvent).findFirst().orElseThrow().equals(voteEventType)).collect(
                        Collectors.toMap(
                            formulaEvent ->
                                Filterable.getTypeSpecificTagsStream(IdentifierTag.class, formulaEvent).findFirst().orElseThrow().getUuid(),
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
    List<BaseTag> list = formulaEvents.stream().map(formulaEvent ->
            Filterable.getTypeSpecificTagsStream(IdentifierTag.class, formulaEvent)
                .findFirst()
                .map(BaseTag.class::cast).orElseThrow())
        .toList();
    return new BadgeAwardReputationEvent(
        aImgIdentity,
        badgeReceiverPubkey,
        badgeDefinitionReputationEvent,
        list,
        new BigDecimal(score));
  }

  @Override
  public String getFullyQualifiedCalculatorName() {
    return getClass().getName();
  }
}
