package com.prosilion.afterimage.calculator;

import com.ezylang.evalex.Expression;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.internal.Relay;
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
  private final String afterimageRelayUrl;

  public DynamicReputationCalculator(
      @NonNull String afterimageRelayUrl,
      @NonNull Identity aImgIdentity) {
    this.aImgIdentity = aImgIdentity;
    this.afterimageRelayUrl = afterimageRelayUrl;
  }

  public BadgeAwardReputationEvent calculateUpdatedReputationEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull BadgeAwardReputationEvent previousReputationEvent,
      @NonNull List<FormulaEvent> formulaEvents,
      @NonNull FollowSetsEvent incomingFollowSetsEvent) throws NostrException {

    List<IdentifierTag> eventVoteTags = incomingFollowSetsEvent.getBadgeAwardGenericEvents()
        .stream().map(BadgeAwardGenericEvent::getBadgeDefinitionGenericEvent)
        .map(BadgeDefinitionGenericEvent::asAddressTag)
        .map(AddressTag::getIdentifierTag)
        .filter(Objects::nonNull)
        .filter(
            formulaEvents.stream()
                .map(FormulaEvent::getBadgeDefinitionGenericEvent)
                .map(BadgeDefinitionGenericEvent::getIdentifierTag).toList()::contains)
        .toList();

    String updatedScore = calculateReputationEventScore(
        eventVoteTags,
        previousReputationEvent,
        formulaEvents);

    BadgeAwardReputationEvent reputationEvent = createReputationEvent(
        voteReceiverPubkey,
        updatedScore,
        previousReputationEvent.getBadgeDefinitionGenericEvent()
    );

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
                        formulaEvent.getBadgeDefinitionGenericEvent().getIdentifierTag().equals(voteEventType)).collect(
                        Collectors.toMap(
                            formulaEvent ->
                                formulaEvent.getBadgeDefinitionGenericEvent().getIdentifierTag().getUuid(),
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

  private BadgeAwardReputationEvent createReputationEvent(
      @NonNull PublicKey badgeReceiverPubkey,
      @NonNull String score,
      @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) throws NostrException {
    BadgeAwardReputationEvent badgeAwardReputationEvent = new BadgeAwardReputationEvent(
        aImgIdentity,
        badgeReceiverPubkey,
        new Relay(afterimageRelayUrl),
        AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG,
        badgeDefinitionReputationEvent,
        new BigDecimal(score));
    return badgeAwardReputationEvent;
  }

  @Override
  public String getFullyQualifiedCalculatorName() {
    return getClass().getName();
  }
}
