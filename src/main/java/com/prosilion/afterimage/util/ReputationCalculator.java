package com.prosilion.afterimage.util;

import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import nostr.event.tag.VoteTag;
import org.springframework.stereotype.Component;

@Component
public class ReputationCalculator {

  public VoteTag calculateReputation(@NonNull List<VoteTag> voteTags) {
    Optional<VoteTag> reduce = voteTags.stream().reduce((voteTag, voteTag2) ->
        new VoteTag(
            voteTag.getVote() + voteTag2.getVote()));
    return reduce.orElseThrow();
  }
}
