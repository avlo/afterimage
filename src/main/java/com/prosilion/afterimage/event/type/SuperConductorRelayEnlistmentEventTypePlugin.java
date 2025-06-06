package com.prosilion.afterimage.event.type;

import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.EventTypePlugin;
import com.prosilion.superconductor.service.event.type.RedisCache;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseTag;
import nostr.event.Kind;
import nostr.event.impl.GenericEvent;
import nostr.event.impl.GroupMembersEvent;
import nostr.event.impl.VoteEvent;
import nostr.id.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SuperConductorRelayEnlistmentEventTypePlugin<T extends GenericEvent> extends AfterImageEventTypePluginIF<T> {
  private final VoteEventTypePlugin<VoteEvent> voteEventTypePlugin;

  @Autowired
  public SuperConductorRelayEnlistmentEventTypePlugin(
      @NonNull RedisCache<T> redisCache,
      @NonNull EventEntityService<T> eventEntityService,
      @NonNull VoteEventTypePlugin<VoteEvent> voteEventTypePlugin,
      @NonNull Identity aImgIdentity) {
    super(redisCache, aImgIdentity, eventEntityService);
    this.voteEventTypePlugin = voteEventTypePlugin;
  }

//  start with pre-defined Map<String, String> superconductorRelays
//  @Autowired
//  public SuperConductorRelayEnlistmentEventTypePlugin(
//      @NonNull RedisCache<T> redisCache,
//      @NonNull EventEntityService<T> eventEntityService,
//      @NonNull VoteEventTypePlugin<VoteEvent> voteEventTypePlugin,
//      @NonNull Identity aImgIdentity,
//      @NonNull Map<String, String> superconductorRelays) throws JsonProcessingException {
//    this(redisCache, eventEntityService, voteEventTypePlugin, aImgIdentity);
//    new SuperconductorMeshProxy<>(superconductorRelays, this.voteEventTypePlugin).setUpReputationReqFlux();
//  }

  @Override
  public T createEvent(@NonNull Identity identity, @NonNull List<BaseTag> tags) {
    log.debug("processing incoming SuperConductorRelayEnlistmentEventTypePlugin event");
    T t = (T) new GroupMembersEvent(
        identity.getPublicKey(),
        tags,
        "");
    identity.sign(t);
    return t;
  }

  @Override
  EventTypePlugin<T> getAbstractEventTypePlugin() {
    return (EventTypePlugin<T>) voteEventTypePlugin;
  }

  @Override
  public Kind getKind() {
    return Kind.GROUP_MEMBERS;
  }
}
