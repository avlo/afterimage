package com.prosilion.afterimage.service.reputation;

import com.prosilion.afterimage.config.ScoreVoteEvents;
import com.prosilion.nostr.NostrException;
import java.math.BigDecimal;

public interface CalculatorServiceIF {
  BigDecimal calculate(ScoreVoteEvents scoreVoteEvents) throws NostrException;
}
