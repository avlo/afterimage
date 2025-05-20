package com.prosilion.afterimage.service.event.type;

import com.prosilion.afterimage.service.event.ReputationEventEntityService;
import com.prosilion.superconductor.entity.EventEntity;
import com.prosilion.superconductor.service.event.type.RedisCache;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import nostr.base.PublicKey;
import nostr.event.impl.GenericEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReputationRedisCache<T extends GenericEvent> {
  private final ReputationEventEntityService<T> reputationEventEntityService;
  @Getter
  private final RedisCache<T> redisCache;

  @Autowired
  public ReputationRedisCache(
      @NonNull ReputationEventEntityService<T> reputationEventEntityService,
      @NonNull RedisCache<T> redisCache) {
    this.reputationEventEntityService = reputationEventEntityService;
    this.redisCache = redisCache;
  }

  protected List<T> getByReferencePublicKey(@NonNull PublicKey publicKey) {
    return reputationEventEntityService.getByReferencePublicKey(publicKey);
  }
}
