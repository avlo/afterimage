//package com.prosilion.afterimage.client;
//
//import com.prosilion.afterimage.service.event.EventServiceIF;
//import com.prosilion.afterimage.util.RelaySubscriptions;
//import java.util.List;
//import java.util.Map;
//import nostr.event.impl.GenericEvent;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//@Component
//public class RelaysAggregate {
//  private final List<RelaySubscriptions> relaySubscriptions;
//
//  @Autowired
//  public RelaysAggregate(EventServiceIF<GenericEvent> eventService, Map<String, String> relays) {
//    this.relaySubscriptions = relays.values().stream().map(RelaySubscriptions::new).toList();
//  }
//
//
//}
////  Map<PubKey, Map<Event<rep-tag (NIP-XXX)>, Relay>
