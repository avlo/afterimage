//package com.prosilion.afterimage.service.event.type;
//
//import com.prosilion.superconductor.service.event.type.EventEntityService;
//import com.prosilion.superconductor.service.event.type.RedisCache;
//import java.util.List;
//import lombok.Getter;
//import lombok.NonNull;
//import nostr.base.PublicKey;
//import nostr.event.impl.GenericEvent;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Service
//public class VoteRedisCache<T extends GenericEvent> {
//  private final EventEntityService<T> eventEntityService;
//  @Getter
//  private final RedisCache<T> redisCache;
//
//  @Autowired
//  public VoteRedisCache(
//      @NonNull EventEntityService<T> eventEntityService,
//      @NonNull RedisCache<T> redisCache) {
//    this.eventEntityService = eventEntityService;
//    this.redisCache = redisCache;
//  }
//
//  protected List<T> getEventsByPublicKey(@NonNull PublicKey publicKey) {
//    List<T> byReferencePublicKey = eventEntityService.findByPublicKey(publicKey);
//    return byReferencePublicKey;
//  }
//}
