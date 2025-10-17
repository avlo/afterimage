package com.prosilion.afterimage.calculator;

import com.ezylang.evalex.Expression;
import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import com.prosilion.superconductor.lib.redis.dto.GenericNosqlEntityKindTypeDto;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class DynamicReputationCalculator implements ReputationCalculatorIF {
  //  private final List<String> VALID_TYPES = List.of(AfterimageKindType.UNIT_UPVOTE.getName(), AfterimageKindType.UNIT_DOWNVOTE.getName());
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;
  private final Identity aImgIdentity;

  public DynamicReputationCalculator(@NonNull Identity aImgIdentity, @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.aImgIdentity = aImgIdentity;
  }

  public EventIF calculateUpdatedReputationEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull Optional<EventIF> previousReputationEvent,
      @NonNull EventIF incomingFollowSetsEvent) throws NostrException {
    return createReputationEvent(
        voteReceiverPubkey,
        new BigDecimal(calculateReputationEventRxR(
            previousReputationEvent
                .map(EventIF::getContent)
                .map(BigDecimal::new)
                .orElse(BigDecimal.ZERO),
            incomingFollowSetsEvent.getTags().stream()
                .filter(AddressTag.class::isInstance)
                .map(AddressTag.class::cast)
                .map(AddressTag::getIdentifierTag)
                .map(identifierTag ->
                    Optional.ofNullable(identifierTag).orElseThrow(() -> new InvalidTagException(
                        "IdentifierTag missing from AddressTag")))
                .map(IdentifierTag::getUuid)
                .toList())));
  }

//  private BigDecimal calculateReputationEvent(BigDecimal previousScore, List<String> voteEvents) throws NostrException {
//    return previousScore.add(
//        calculateReputationLoop(
//            voteEvents.stream()
//                .map(this::convertContentToScore).toList()));
//  }

  private String calculateReputationEventRxR(BigDecimal previousScore, List<String> voteEvents) {
    String reduce = voteEvents.stream().map(voteEventType ->
            Filterable.getTypeSpecificTagsStream(ExternalIdentityTag.class, reputationBadgeDefinitionEvent).collect(
                Collectors.toMap(
                    externalIdentityTag -> externalIdentityTag.getIdentifierTag().getUuid(),
                    ExternalIdentityTag::getFormula,
                    (prev, next) -> next, HashMap::new)).get(voteEventType.toUpperCase()))
        .reduce(previousScore.toString(), this::doCalc);
    return reduce;
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

//  private BigDecimal calculateReputationLoop(List<BigDecimal> uuids) {
//    return uuids.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
//  }

  private GenericEventKindTypeIF createReputationEvent(@NonNull PublicKey badgeReceiverPubkey, @NonNull BigDecimal score) throws NostrException {
    return new GenericNosqlEntityKindTypeDto(
        new BadgeAwardReputationEvent(
            aImgIdentity,
            badgeReceiverPubkey,
            reputationBadgeDefinitionEvent,
            List.of(
                new IdentifierTag(
                    getFullyQualifiedCalculatorName())),
            score),
        AfterimageKindType.UNIT_REPUTATION).convertBaseEventToGenericEventKindTypeIF();
  }

//  private BigDecimal convertContentToScore(String event) {
//    if (!VALID_TYPES.contains(event)) {

  /// /      TODO: replace unchecked excpetion with proper client notification
//      throw new IllegalArgumentException(new InvalidTagException(event, VALID_TYPES).getMessage());
//    }
//    return event.equals(SuperconductorKindType.UNIT_UPVOTE.getName()) ? new BigDecimal("1") : new BigDecimal("-1");
//  }
  @Override
  public String getFullyQualifiedCalculatorName() {
    return getClass().getName();
  }
}
