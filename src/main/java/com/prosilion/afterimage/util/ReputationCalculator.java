package com.prosilion.afterimage.util;

import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import nostr.event.tag.VoteTag;
import org.springframework.stereotype.Component;

@Component
public class ReputationCalculator {

  public Integer calculateReputation(@NonNull List<VoteTag> voteTags) {
    Integer reduce = voteTags.stream()
        .map(VoteTag::getVote)
        .reduce(Integer::sum).orElse(0);
    return reduce;
  }
}
