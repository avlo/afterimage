package com.prosilion.afterimage.service.event.type;

import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.ReputationCalculator;
import com.prosilion.superconductor.service.event.type.AbstractEventTypePlugin;
import com.prosilion.superconductor.service.event.type.DeleteEventTypePlugin;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.RedisCache;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.Kind;
import nostr.event.impl.DeletionEvent;
import nostr.event.impl.GenericEvent;
import nostr.event.tag.AddressTag;
import nostr.event.tag.VoteTag;
import nostr.id.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VoteEventTypePlugin<T extends GenericEvent, U extends DeletionEvent> extends AbstractEventTypePlugin<T> {
  //  public static final String CONTENT = "AIMG REPUTATION SCORE EVENT";
  private final Identity afterimageInstanceIdentity;
  private final ReputationCalculator reputationCalculator;
  private final EventEntityService<T> eventEntityService;
  private final DeleteEventTypePlugin<U> deleteEventTypePlugin;

  @Autowired
  public VoteEventTypePlugin(
      @NonNull RedisCache<T> redisCache,
      @NonNull ReputationCalculator reputationCalculator,
      @NonNull EventEntityService<T> eventEntityService,
      @NonNull DeleteEventTypePlugin<U> deleteEventTypePlugin,
      @NonNull Identity afterimageInstanceIdentity) {
    super(redisCache);
    this.deleteEventTypePlugin = deleteEventTypePlugin;
    this.eventEntityService = eventEntityService;
    this.reputationCalculator = reputationCalculator;
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
  }

  @Override
  public void processIncomingEvent(@NonNull T event) {
    log.debug("processing incoming REPUTATION EVENT: [{}]", event);

//    existing votes
    Integer reputationScore = reputationCalculator.calculateReputation(
        eventEntityService
            .getEventsByPublicKey(event.getPubKey()).stream().filter(e ->
                e.getKind().equals(Kind.VOTE.getValue()))
            .map(this::voteTagFilter)
            .flatMap(Collection::stream).toList());

//    delete old reputation event
    AddressTag deletedScoreAddressTag = new AddressTag(
        Kind.REPUTATION.getValue(),
        event.getPubKey()
//        , new IdentifierTag(String.format("REPUTATION_UUID-%s", reputationScore - 1))
    );
    U deletionEvent = Factory.createDeletionEvent(afterimageInstanceIdentity, reputationScore - 1, deletedScoreAddressTag);
    deleteEventTypePlugin.processIncomingEvent(deletionEvent);

//    create new reputation event    
    AddressTag updatedScoreAddressTag = new AddressTag(
        Kind.REPUTATION.getValue(),
        event.getPubKey()
//        , new IdentifierTag(String.format("REPUTATION_UUID-%s", reputationScore))
    );
    T reputationEvent = Factory.createReputationEvent(afterimageInstanceIdentity, reputationScore, updatedScoreAddressTag);
    save(reputationEvent);
  }

  private List<VoteTag> voteTagFilter(T event) {
    return event
        .getTags().stream()
        .filter(VoteTag.class::isInstance)
        .map(VoteTag.class::cast)
        .toList();
  }

  @Override
  public Kind getKind() {
    return Kind.VOTE;
  }
}
