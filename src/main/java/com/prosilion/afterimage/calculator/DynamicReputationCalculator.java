package com.prosilion.afterimage.calculator;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.AddressableEvent;
import com.prosilion.nostr.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
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
import java.util.stream.Collectors;
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

    List<IdentifierTag> formulaUuids = incomingFollowSetsEvent
        .getBadgeDefinitionReputationEvent()
        .getFormulaEvents().stream().map(AddressableEvent::asAddressableEventAddressTag)
        .filter(
            formulaEvents.stream()
                .map(FormulaEvent::asAddressableEventAddressTag)
                .toList()::contains)
        .distinct().map(AddressTag::getIdentifierTag)
        .toList();

    String updatedScore = calculateReputationEventScore(
        formulaUuids,
        previousReputationEvent,
        formulaEvents);

    BadgeAwardReputationEvent reputationEvent = createReputationEvent(
        voteReceiverPubkey,
        updatedScore,
        previousReputationEvent.getBadgeDefinitionEvent()
    );

    return reputationEvent;
  }

  private String calculateReputationEventScore(
      List<IdentifierTag> formulaUuids,
      BadgeAwardReputationEvent previousReputationEvent,
      List<FormulaEvent> formulaEvents) {
    String result =
        formulaUuids.stream().map(formulaUuid ->
                formulaEvents.stream()
                    .filter(formulaEvent ->
                        formulaEvent.getIdentifierTag().equals(formulaUuid)).collect(
                        Collectors.toMap(
                            formulaEvent ->
                                formulaEvent.getIdentifierTag().getUuid(),
                            FormulaEvent::getFormula,
                            (prev, next) -> next, HashMap::new)).get(formulaUuid.getUuid()))
            .reduce(previousReputationEvent.getScore(), ExpressionCalculator::calculate);
    return result;
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
