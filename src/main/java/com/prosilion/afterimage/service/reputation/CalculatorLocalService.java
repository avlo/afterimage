package com.prosilion.afterimage.service.reputation;

import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.config.ScoreVoteEvents;
import com.prosilion.nostr.NostrException;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import java.math.BigDecimal;
import java.util.List;

public class CalculatorLocalService implements CalculatorServiceIF {
  public static final List<String> VALID_TYPES = List.of(SuperconductorKindType.UNIT_UPVOTE.getName(), SuperconductorKindType.UNIT_DOWNVOTE.getName());

  @Override
  public BigDecimal calculate(ScoreVoteEvents scoreVoteEvents) throws NostrException {
    return scoreVoteEvents.previousScore().add(
        calculateReputationLoop(
            scoreVoteEvents.voteEvents().stream()
                .map(CalculatorLocalService::convertContentToScore).toList()));
  }

  private static BigDecimal calculateReputationLoop(List<BigDecimal> uuids) {
    return uuids.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static BigDecimal convertContentToScore(String event) {
    if (!VALID_TYPES.contains(event)) {
//      TODO: replace unchecked excpetion with proper client notification
      throw new IllegalArgumentException(new InvalidTagException(event, VALID_TYPES).getMessage());
    }
    return event.equals(SuperconductorKindType.UNIT_UPVOTE.getName()) ? new BigDecimal("1") : new BigDecimal("-1");
  }
}
