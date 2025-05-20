package com.prosilion.afterimage.service.event.type;

import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.ReputationCalculator;
import com.prosilion.superconductor.service.event.type.AbstractEventTypePlugin;
import com.prosilion.superconductor.service.event.type.EventTypePlugin;
import java.util.ArrayList;
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
public class ReputationEventTypePlugin<T extends GenericEvent> extends AbstractEventTypePlugin<T> implements EventTypePlugin<T> {
  public static final String CONTENT = "TEMP LOCATION OF AIMG REPUTATION SCORE";
  private final Identity afterimageInstanceIdentity;
  private final ReputationCalculator reputationCalculator;
  private final ReputationRedisCache<T> reputationRedisCache;

  @Autowired
  public ReputationEventTypePlugin(
      @NonNull ReputationCalculator reputationCalculator,
      @NonNull ReputationRedisCache<T> reputationRedisCache,
      @NonNull Identity afterimageInstanceIdentity) {
    super(reputationRedisCache.getRedisCache());
    this.reputationRedisCache = reputationRedisCache;
    this.reputationCalculator = reputationCalculator;
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
  }

  @Override
  public void processIncomingEvent(@NonNull T event) {
    log.debug("processing incoming CANONICAL EVENT: [{}]", event);

    List<T> byReferencePublicKey = reputationRedisCache.getByReferencePublicKey
        (event.getPubKey());

    List<VoteTag> voteTags = byReferencePublicKey.stream()
        .map(this::voteTagFilter)
        .toList();

    VoteTag reputationScore = reputationCalculator.calculateReputation(voteTags);

    List<BaseTag> tags = new ArrayList<>();
    tags.add(reputationScore);

    GenericEvent textNoteEvent = Factory.createTextNoteEvent(afterimageInstanceIdentity, tags, CONTENT);
    textNoteEvent.setKind(getKind().getValue());
    afterimageInstanceIdentity.sign(textNoteEvent);

    save(event);
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
