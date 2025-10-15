package com.prosilion.afterimage.config;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.lang.NonNull;

public record ScoreVoteEvents(@NonNull BigDecimal previousScore, @NonNull List<String> voteEvents) {
}
