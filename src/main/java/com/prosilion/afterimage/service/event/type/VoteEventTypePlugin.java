package com.prosilion.afterimage.service.event.type;

import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.ReputationCalculator;
import com.prosilion.superconductor.service.event.type.AbstractEventTypePlugin;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseTag;
import nostr.event.Kind;
import nostr.event.impl.GenericEvent;
import nostr.event.tag.VoteTag;
import nostr.id.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VoteEventTypePlugin<T extends GenericEvent> extends AbstractEventTypePlugin<T> {
  public static final String CONTENT = "AIMG REPUTATION SCORE EVENT";
  private final Identity afterimageInstanceIdentity;
  private final ReputationCalculator reputationCalculator;
  private final VoteRedisCache<T> voteRedisCache;

  @Autowired
  public VoteEventTypePlugin(
      @NonNull ReputationCalculator reputationCalculator,
      @NonNull VoteRedisCache<T> voteRedisCache,
      @NonNull Identity afterimageInstanceIdentity) {
    super(voteRedisCache.getRedisCache());
    this.voteRedisCache = voteRedisCache;
    this.reputationCalculator = reputationCalculator;
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
  }

  @Override
  public void processIncomingEvent(@NonNull T event) {
    log.debug("processing incoming REPUTATION EVENT: [{}]", event);

    List<T> eventsByPublicKey = voteRedisCache.getEventsByPublicKey(event.getPubKey());

    List<T> eventsByPublicKeyByVoteKind = eventsByPublicKey.stream().filter(e -> e.getKind().equals(Kind.VOTE.getValue())).toList();

    List<VoteTag> publicKeyVoteTags = eventsByPublicKeyByVoteKind.stream()
        .map(this::voteTagFilter)
        .toList();

    Integer reputationScore = reputationCalculator.calculateReputation(publicKeyVoteTags);

    List<BaseTag> voteTags = List.of(new VoteTag(reputationScore));

    T reputationEvent = Factory.createReputationEvent(afterimageInstanceIdentity, voteTags, CONTENT);
    afterimageInstanceIdentity.sign(reputationEvent);

    save(reputationEvent);
  }

  private VoteTag voteTagFilter(T event) {
    VoteTag voteTag = event
        .getTags().stream()
        .filter(VoteTag.class::isInstance)
        .findFirst()
        .map(VoteTag.class::cast).orElseThrow();
    return voteTag;
  }

  @Override
  public Kind getKind() {
    return Kind.VOTE;
  }
}
