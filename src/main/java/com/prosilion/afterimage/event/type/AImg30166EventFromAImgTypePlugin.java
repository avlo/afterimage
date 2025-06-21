//package com.prosilion.afterimage.event.type;
//
//import com.prosilion.afterimage.event.ReputationEvent;
//import com.prosilion.afterimage.util.ReputationCalculator;
//import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventTypePlugin;
//import com.prosilion.superconductor.service.event.type.EventEntityService;
//import com.prosilion.superconductor.service.event.type.RedisCache;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//import lombok.NonNull;
//import lombok.extern.slf4j.Slf4j;
//import nostr.event.BaseTag;
//import nostr.event.Kind;
//import nostr.event.filter.Filterable;
//import nostr.event.impl.GenericEvent;
//import nostr.event.impl.VoteEvent;
//import nostr.event.tag.AddressTag;
//import nostr.event.tag.IdentifierTag;
//import nostr.event.tag.VoteTag;
//import nostr.id.Identity;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class AImg30166EventFromAImgTypePlugin<T extends GenericEvent> extends AbstractNonPublishingEventTypePlugin<T> {
//  private final Identity aImgIdentity;
//  private final EventEntityService<T> eventEntityService;
//  private final ReputationEventTypePlugin<T> reputationEventTypePlugin;
//
//  @Autowired
//  public AImg30166EventFromAImgTypePlugin(
//      @NonNull RedisCache<T> redisCache,
//      @NonNull EventEntityService<T> eventEntityService,
//      @NonNull ReputationEventTypePlugin<T> reputationEventTypePlugin,
//      @NonNull Identity aImgIdentity) {
//    super(redisCache);
//    this.aImgIdentity = aImgIdentity;
//    this.eventEntityService = eventEntityService;
//    this.reputationEventTypePlugin = reputationEventTypePlugin;
//  }
//
//  @Override
//  public void processIncomingNonPublishingEventType(@NonNull T relayDiscoveryEvent) {
//    log.debug("RelayDiscoveryEventTypePlugin processing incoming Kind.RELAY_DISCOVERY 30166 : [{}]", relayDiscoveryEvent);
///*{
//  "id": "<eventid>",
//  "pubkey": "AIMG pubkey>",
//  "created_at": "<created_at  [some recent date ...]>",
//  "signature": "<signature>",
//  "content": "{}",
//  "kind": 30166,
//  "tags": [
//    ["d","ws://aimg.url:port"], <------  d-tag URI must be at index[1]
//    // ["n", "clearnet"],
//    ["T", "PublicOutbox" ]
//    ["T", "Broadcast" ]
//    ["vote", "VOTE_RECIP_1_PUBKEY", "vote_n_event_id", INTEGER_VOTE, "vote's SC source ws://sc.url:port"],
//    ["vote", "VOTE_RECIP_1_PUBKEY", "vote_n+1_event_id", INTEGER_VOTE, "vote's SC source ws://sc.url:port"],
//    ["vote", "VOTE_RECIP_1_PUBKEY", "vote_n+m_event_id", INTEGER_VOTE, "vote's SC source ws://sc.url:port"],
//    ...
//    ["vote", "VOTE_RECIP_2_PUBKEY", "vote_n_event_id", INTEGER_VOTE, "vote's SC source ws://sc.url:port"],
//    ["vote", "VOTE_RECIP_2_PUBKEY", "vote_n+1_event_id", INTEGER_VOTE, "vote's SC source ws://sc.url:port"],
//    ["vote", "VOTE_RECIP_2_PUBKEY", "vote_n+m_event_id", INTEGER_VOTE, "vote's SC source ws://sc.url:port"],
//    ...
//  ]}*/
//
////	if aImg already has 30166 EVENT containing same dTag (aka, relay_url), that means we already have that relay_url (and all it's votes) registered, so return (do nothing)
//    IdentifierTag inomingIdentifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, relayDiscoveryEvent).getFirst();
//    List<IdentifierTag> existingIdentifierTags = eventEntityService
//        .getEventsByKind(Kind.RELAY_DISCOVERY)
//        .stream().map(kind30166Event ->
//            Filterable.getTypeSpecificTags(IdentifierTag.class, kind30166Event))
//        .findFirst().orElseThrow();
//
////     a1. if existing 30166 event with same dTag exists, return (do nothing, we already have that aImg relay)
//    if (existingIdentifierTags.contains(inomingIdentifierTag))
//      return;
//
////	otherwise, aImg doesn't yet have 30166 EVENT containing same dTag, so:
////	 	 for each unique aTag's pubKey
//    Map<String, List<String>> pubKeyVotesMap = Filterable.getTypeSpecificTags(
//            AddressTag.class, relayDiscoveryEvent).stream()
//        .collect(
//            Collectors.toMap(addressTag -> addressTag.getPublicKey().toHexString(),
//                addressTag -> Collections.singletonList(addressTag.getIdentifierTag().getUuid())));
//
//    //	   	3a. do REPUTATION calculation
//    Map<String, String> pubKeyReputationMap = pubKeyVotesMap.entrySet().stream().collect(
//        Collectors.toMap(Map.Entry::getKey, entry ->
//            entry.getValue().stream().reduce((s, s2) -> s + s2).orElseThrow()));
//
//    
////	   	3b. save REPUTATION EVENT
////	 	 	3a. save 30166 event
////	 	 	3d. submit VOTE ReqMessage Filter(Kind.VOTE, Pubkey) to dTag    
//
//  }
//
//
//  private T calculateReputationEvent(T event) {
//    return createReputationEvent(
//        aImgIdentity,
//        ReputationCalculator.calculateReputation(
//            eventEntityService
//                .getEventsByPublicKey(event.getPubKey()).stream().filter(e ->
//                    e.getKind().equals(Kind.VOTE.getValue()))
//                .map(voteEvent -> voteEvent.getTags().stream()
//                    .filter(VoteTag.class::isInstance)
//                    .map(VoteTag.class::cast).toList())
//                .flatMap(Collection::stream).toList()),
//        new AddressTag(
//            Kind.REPUTATION.getValue(),
//            event.getPubKey(),
//            new IdentifierTag(
////  TODO:  temp working POC below using identity pubkey, proper sol'n needs arch/design revisit                
//                aImgIdentity.getPublicKey().toHexString()
////                String.valueOf(System.currentTimeMillis())
//            ) // UUID
//        ));
//  }
//
//  private T createReputationEvent(@NonNull Identity identity, @NonNull Integer score, @NonNull BaseTag tag) {
//    return createReputationEvent(identity, score, List.of(tag));
//  }
//
//  private T createReputationEvent(@NonNull Identity identity, @NonNull Integer score, @NonNull List<BaseTag> tags) {
//    T t = (T) new ReputationEvent(
//        identity.getPublicKey(),
//        tags,
//        score.toString());
//    identity.sign(t);
//    return t;
//  }
//
//  @Override
//  public Kind getKind() {
//    return Kind.RELAY_DISCOVERY; // 30166
//  }
//}
