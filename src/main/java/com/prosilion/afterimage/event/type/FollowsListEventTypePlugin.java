package com.prosilion.afterimage.event.type;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.relay.SuperconductorMeshProxy;
import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventTypePlugin;
import com.prosilion.superconductor.service.event.type.RedisCache;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.Kind;
import nostr.event.filter.Filterable;
import nostr.event.impl.GenericEvent;
import nostr.event.impl.VoteEvent;
import nostr.event.tag.PubKeyTag;
import org.apache.commons.lang3.stream.Streams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FollowsListEventTypePlugin<T extends GenericEvent> extends AbstractNonPublishingEventTypePlugin<T> {
  private final VoteEventTypePlugin<VoteEvent> voteEventTypePlugin;

  @Autowired
  public FollowsListEventTypePlugin(
      @NonNull RedisCache<T> redisCache,
      @NonNull VoteEventTypePlugin<VoteEvent> voteEventTypePlugin,
      @NonNull Map<String, String> superconductorRelays) throws JsonProcessingException {
    super(redisCache);
    this.voteEventTypePlugin = voteEventTypePlugin;
    new SuperconductorMeshProxy<>(superconductorRelays, this.voteEventTypePlugin).setUpReputationReqFlux();
  }

  @Override
  public void processIncomingNonPublishingEventType(@NonNull T followsListEvent) {
    log.debug("processing incoming FOLLOWS LIST event: [{}]", followsListEvent);

    save(followsListEvent);

    followsListEvent.getTags().forEach(tag ->
        Streams
            .failableStream(
                Filterable.getTypeSpecificTags(PubKeyTag.class, followsListEvent))
            .map(pubKeyTag -> Map.of(
                pubKeyTag.getPublicKey().toHexString(), pubKeyTag.getMainRelayUrl()))
            .forEach(p ->
                new SuperconductorMeshProxy<>(p, voteEventTypePlugin).setUpReputationReqFlux()));
  }

  @Override
  public Kind getKind() {
    return Kind.CONTACT_LIST;
  }
}
