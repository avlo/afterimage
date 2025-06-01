package com.prosilion.afterimage.event.type;

import com.prosilion.afterimage.event.ReputationEvent;
import com.prosilion.afterimage.util.ReputationCalculator;
import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventTypePlugin;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.RedisCache;
import com.prosilion.superconductor.service.request.NotifierService;
import com.prosilion.superconductor.service.request.pubsub.AddNostrEvent;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseTag;
import nostr.event.Kind;
import nostr.event.impl.GenericEvent;
import nostr.event.tag.AddressTag;
import nostr.event.tag.VoteTag;
import nostr.id.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VoteEventTypePlugin<T extends GenericEvent> extends AbstractNonPublishingEventTypePlugin<T> {
  private final Identity aImgIdentity;
  private final EventEntityService<T> eventEntityService;
  private final NotifierService<T> notifierService;

  @Autowired
  public VoteEventTypePlugin(
      @NonNull RedisCache<T> redisCache,
      @NonNull EventEntityService<T> eventEntityService,
      @NonNull NotifierService<T> notifierService,
      @NonNull Identity aImgIdentity) {
    super(redisCache);
    this.eventEntityService = eventEntityService;
    this.notifierService = notifierService;
    this.aImgIdentity = aImgIdentity;
  }

  @Override
  public void processIncomingNonPublishingEventType(@NonNull T voteEvent) {
    log.debug("processing incoming VOTE EVENT: [{}]", voteEvent);
//    saves VOTE event without triggering subscriber listener
    save(voteEvent);

    T reputationEvent = calculateReputationEvent(voteEvent);
    save(reputationEvent);

    notifierService.nostrEventHandler(new AddNostrEvent<>(reputationEvent));
  }

  private T calculateReputationEvent(T event) {
    return createReputationEvent(
        aImgIdentity,
        ReputationCalculator.calculateReputation(
            eventEntityService
                .getEventsByPublicKey(event.getPubKey()).stream().filter(e ->
                    e.getKind().equals(Kind.VOTE.getValue()))
                .map(voteEvent -> voteEvent.getTags().stream()
                    .filter(VoteTag.class::isInstance)
                    .map(VoteTag.class::cast).toList())
                .flatMap(Collection::stream).toList()),
//    non-parameterized replaceable variant:
//      ["a", <kind integer>:<32-bytes lowercase hex of a pubkey>:]
        new AddressTag(
            Kind.REPUTATION.getValue(),
            event.getPubKey()
//    or potentially/optionally parameterized replaceable:
//      ["a", <kind integer>:<32-bytes lowercase hex of a pubkey>:<d tag value>]
//    by uncommenting below line
//        , new IdentifierTag(String.format("REPUTATION_UUID-%s", reputationScore))
        ));
  }

  @Override
  public Kind getKind() {
    return Kind.VOTE;
  }

  private T createReputationEvent(@NonNull Identity identity, @NonNull Integer score, @NonNull BaseTag tag) {
    return createReputationEvent(identity, score, List.of(tag));
  }

  private T createReputationEvent(@NonNull Identity identity, @NonNull Integer score, @NonNull List<BaseTag> tags) {
    T t = (T) new ReputationEvent(
        identity.getPublicKey(),
        tags,
        score.toString());
    identity.sign(t);
    return t;
  }
}
