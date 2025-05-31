package com.prosilion.afterimage.util;

import java.util.List;
import lombok.NonNull;
import nostr.event.tag.VoteTag;

public class ReputationCalculator {

  public static Integer calculateReputation(@NonNull List<VoteTag> voteTags) {
    return voteTags.stream()
        .map(VoteTag::getVote)
        .reduce(Integer::sum).orElse(0);
  }
}
