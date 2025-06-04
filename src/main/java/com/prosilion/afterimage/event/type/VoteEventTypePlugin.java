package com.prosilion.afterimage.event.type;

import com.prosilion.afterimage.event.ReputationEvent;
import com.prosilion.afterimage.util.ReputationCalculator;
import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventTypePlugin;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.RedisCache;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseTag;
import nostr.event.Kind;
import nostr.event.impl.GenericEvent;
import nostr.event.tag.AddressTag;
import nostr.event.tag.IdentifierTag;
import nostr.event.tag.VoteTag;
import nostr.id.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
// TODO: potentially option/alternative for ReputationEvent (extends NIP01Event, Nip-2113):
//  Event Count (Nip-45)
//  REQ format:
//    ["COUNT", <subscription_id>, {"kinds": [3], "#p": [<pubkey>]}]
//  RESPONSE (is non-event)
//    ["COUNT", <subscription_id>, {"count": 238}]
//  note, however: COUNT does not carry event data, which is/might be contextual shortcoming

public class VoteEventTypePlugin<T extends GenericEvent> extends AbstractNonPublishingEventTypePlugin<T> {
  private final Identity aImgIdentity;
  private final EventEntityService<T> eventEntityService;
  private final ReputationEventTypePlugin<T> reputationEventTypePlugin;

  @Autowired
  public VoteEventTypePlugin(
      @NonNull RedisCache<T> redisCache,
      @NonNull EventEntityService<T> eventEntityService,
      @NonNull ReputationEventTypePlugin<T> reputationEventTypePlugin,
      @NonNull Identity aImgIdentity) {
    super(redisCache);
    this.eventEntityService = eventEntityService;
    this.reputationEventTypePlugin = reputationEventTypePlugin;
    this.aImgIdentity = aImgIdentity;
  }

  @Override
  public void processIncomingNonPublishingEventType(@NonNull T voteEvent) {
    log.debug("processing incoming VOTE EVENT: [{}]", voteEvent);
//    saves VOTE event without triggering subscriber listener
    save(voteEvent);

    T reputationEvent = calculateReputationEvent(voteEvent);
    save(reputationEvent);

    reputationEventTypePlugin.processIncomingEvent(reputationEvent);
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
        new AddressTag(
            Kind.REPUTATION.getValue(),
            event.getPubKey(), 
            new IdentifierTag(
//  TODO:  temp working POC below using identity pubkey, proper sol'n needs arch/design revisit                
                aImgIdentity.getPublicKey().toHexString()
//                String.valueOf(System.currentTimeMillis())
            ) // UUID
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
