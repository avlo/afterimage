//package com.prosilion.afterimage.event.type;
//
//import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventTypePlugin;
//import com.prosilion.superconductor.service.event.type.EventEntityService;
//import com.prosilion.superconductor.service.event.type.RedisCache;
//import lombok.Getter;
//import lombok.NonNull;
//import lombok.extern.slf4j.Slf4j;
//import nostr.event.impl.GenericEvent;
//import nostr.id.Identity;
//
//@Slf4j
//@Getter
//public abstract class AfterImageNonPublishingEventTypePluginIF<T extends GenericEvent> extends AbstractNonPublishingEventTypePlugin<T> {
//
//  private final EventEntityService<T> eventEntityService;
//  private final Identity aImgIdentity;
//
//  public AfterImageNonPublishingEventTypePluginIF(
//      @NonNull RedisCache<T> redisCache,
//      @NonNull Identity aImgIdentity,
//      @NonNull EventEntityService<T> eventEntityService) {
//    super(redisCache);
//    this.aImgIdentity = aImgIdentity;
//    this.eventEntityService = eventEntityService;
//  }
//}
