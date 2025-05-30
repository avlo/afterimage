//package com.prosilion.afterimage.service.event;
//
//import com.prosilion.superconductor.entity.EventEntity;
//import com.prosilion.superconductor.service.event.type.EventEntityService;
//import java.util.List;
//import lombok.NonNull;
//import nostr.base.PublicKey;
//import nostr.event.impl.GenericEvent;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Service
//public class VoteEventEntityService<T extends GenericEvent> {
//  private final EventEntityService<T> eventEntityService;
//
//  @Autowired
//  public VoteEventEntityService(
//      @NonNull EventEntityService<T> eventEntityService) {
//    this.eventEntityService = eventEntityService;
//  }
//
//  public List<T> findByPublicKey(@NonNull PublicKey publicKey) {
//    List<T> list = eventEntityService.findByPublicKey(publicKey);
//    return list;
//  }
//}
