package com.prosilion.afterimage.calculator;

import com.ezylang.evalex.Expression;
import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.tag.ExternalIdentityTag;
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
      @NonNull BadgeAwardReputationEvent dbPreviousReputationEvent,
      @NonNull EventIF incomingFollowSetsEvent) throws NostrException {

    String definitionUuids = dbPreviousReputationEvent
        .getBadgeDefinitionReputationEvent()
        .getIdentifierTag()
//        .getExternalIdentityTags().stream()
//        .map(
//            externalIdentityTag -> externalIdentityTag.getIdentifierTag().getUuid()).toList();
        .getUuid();

    List<BaseTag> allEventTags = incomingFollowSetsEvent.getTags();

    List<String> eventVoteTags = allEventTags.stream()
        .filter(AddressTag.class::isInstance)
        .map(AddressTag.class::cast)
        .map(AddressTag::getIdentifierTag)
        .filter(Objects::nonNull)
        .map(IdentifierTag::getUuid)
        .filter(definitionUuids::contains)
        .toList();

    String updatedScore = calculateReputationEvent(eventVoteTags, dbPreviousReputationEvent);

    EventIF reputationEvent = createReputationEvent(voteReceiverPubkey, updatedScore, dbPreviousReputationEvent.getBadgeDefinitionReputationEvent());
    return reputationEvent;
  }

  private String calculateReputationEvent(
      List<String> voteEvents,
      BadgeAwardReputationEvent previousReputationEvent) {
    return voteEvents.stream().map(voteEventType ->
            Filterable.getTypeSpecificTagsStream(ExternalIdentityTag.class, previousReputationEvent.getBadgeDefinitionReputationEvent()).collect(
                Collectors.toMap(
                    externalIdentityTag -> externalIdentityTag.getIdentifierTag().getUuid(),
                    ExternalIdentityTag::getFormula,
                    (prev, next) -> next, HashMap::new)).get(voteEventType.toUpperCase()))
        .reduce(previousReputationEvent.getContent(), this::doCalc);
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

  private EventIF createReputationEvent(
      @NonNull PublicKey badgeReceiverPubkey,
      @NonNull String score,
      @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) throws NostrException {
    return new BadgeAwardReputationEvent(
        aImgIdentity,
        badgeReceiverPubkey,
        badgeDefinitionReputationEvent,
        List.of(
            new IdentifierTag(
                getFullyQualifiedCalculatorName())),
        new BigDecimal(score));
  }

  @Override
  public String getFullyQualifiedCalculatorName() {
    return getClass().getName();
  }
}
