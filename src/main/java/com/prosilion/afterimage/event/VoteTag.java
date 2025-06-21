//package com.prosilion.afterimage.event;
//
//import lombok.NonNull;
//import nostr.base.PublicKey;
//import nostr.base.Relay;
//import nostr.event.Kind;
//import nostr.event.impl.AbstractBadgeAwardEvent;
//import nostr.event.tag.AddressTag;
//import nostr.event.tag.IdentifierTag;
//
//public class VoteTag extends AddressTag {
//
//  public VoteTag(@NonNull PublicKey issuerPubKey, AbstractBadgeAwardEvent.Type type, @NonNull Relay relay) {
//    super(
//        Kind.BADGE_AWARD_EVENT.getValue(),
//        issuerPubKey,
//        new IdentifierTag(
//            type.getName()), relay);
//  }
//}
