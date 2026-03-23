//package com.prosilion.afterimage.service.event.plugin;
//
//import com.prosilion.afterimage.calculator.DynamicReputationCalculator;
//import com.prosilion.nostr.NostrException;
//import com.prosilion.nostr.enums.Kind;
//import com.prosilion.nostr.event.DeletionEvent;
//import com.prosilion.nostr.event.EventIF;
//import com.prosilion.nostr.event.FollowSetsEvent;
//import com.prosilion.nostr.event.GenericEventRecord;
//import com.prosilion.nostr.event.internal.Relay;
//import com.prosilion.nostr.filter.Filterable;
//import com.prosilion.nostr.tag.EventTag;
//import com.prosilion.nostr.tag.IdentifierTag;
//import com.prosilion.nostr.tag.PubKeyTag;
//import com.prosilion.nostr.user.Identity;
//import com.prosilion.nostr.user.PublicKey;
//import com.prosilion.superconductor.autoconfigure.base.service.event.award.CacheBadgeAwardGenericEventService;
//import com.prosilion.superconductor.base.cache.CacheFollowSetsEventServiceIF;
//import com.prosilion.superconductor.base.cache.CacheServiceIF;
//import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
//import com.prosilion.superconductor.base.service.event.plugin.kind.PublishingEventKindPlugin;
//import com.prosilion.superconductor.base.service.event.plugin.kind.type.EventKindTypePluginIF;
//import com.prosilion.superconductor.base.service.request.subscriber.NotifierService;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Stream;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.lang.NonNull;
//
//@Slf4j
//public class AfterimageFollowSetsEventKindPluginRxR extends PublishingEventKindPlugin { // kind 30_000
//  private final Identity aImgIdentity;
//  private final CacheServiceIF cacheServiceIF;
//  private final CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF;
//  private final CacheBadgeAwardGenericEventService cacheBadgeAwardGenericEventService;
//  private final EventKindTypePluginIF reputationEventPlugin;
//  private final Relay relay;
//
//  public AfterimageFollowSetsEventKindPluginRxR(
//      @NonNull String afterimageRelayUrl,
//      @NonNull NotifierService notifierService,
//      @NonNull EventPlugin eventPlugin,
//      @NonNull CacheServiceIF cacheServiceIF,
//      @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
//      @NonNull CacheBadgeAwardGenericEventService cacheBadgeAwardGenericEventService,
//      @NonNull Identity aImgIdentity,
//      @NonNull EventKindTypePluginIF badgeAwardReputationEventKindTypePlugin) {
//    super(notifierService, eventPlugin);
//    this.aImgIdentity = aImgIdentity;
//    this.cacheServiceIF = cacheServiceIF;
//    this.cacheFollowSetsEventServiceIF = cacheFollowSetsEventServiceIF;
//    this.cacheBadgeAwardGenericEventService = cacheBadgeAwardGenericEventService;
//    this.reputationEventPlugin = badgeAwardReputationEventKindTypePlugin;
//    this.relay = new Relay(afterimageRelayUrl);
//  }
//
//  @Override
//  public GenericEventRecord processIncomingEvent(@NonNull EventIF incomingFollowSetsEvent) {
//    log.debug("processing incoming Kind[{}]: {}\nincomingFollowSetsEvent:\n  {}",
//        incomingFollowSetsEvent.getKind().getValue(),
//        incomingFollowSetsEvent.getKind().getName().toUpperCase(),
//        incomingFollowSetsEvent.createPrettyPrintJson());
//
//    FollowSetsEvent materializedIncomingFollowSetsEvent =
//        new FollowSetsEvent(incomingFollowSetsEvent.asGenericEventRecord());
//
//    log.debug("materialized incoming FollowSetsEvent\n  {}", materializedIncomingFollowSetsEvent.createPrettyPrintJson());
//
//    PublicKey voteReceiverPubkey = Filterable.getTypeSpecificTagsStream(PubKeyTag.class, materializedIncomingFollowSetsEvent).findFirst().orElseThrow(() ->
//        new NostrException("incomingFollowSetsEvent did not contain a PubKeyTag")).getPublicKey();
//
//    Optional<GenericEventRecord> eventsByKindAndPubKeyTagAndIdentifierTag = cacheServiceIF.getEventsByKindAndPubKeyTagAndIdentifierTag(
//        Kind.FOLLOW_SETS,
//        voteReceiverPubkey,
//        materializedIncomingFollowSetsEvent.getIdentifierTag()).stream().findFirst();
//
//    Optional<FollowSetsEvent> existingFollowSetsEvent = eventsByKindAndPubKeyTagAndIdentifierTag.stream()
//        .map(genericEventRecord ->
//            new FollowSetsEvent(genericEventRecord))
//        .findFirst();
//
////    TODO: ifPresent likely superfluous if delete mechanism already handles optional
//    existingFollowSetsEvent.ifPresent(this::deletePreviousFollowSetsEvent);
//
//    List<EventTag> existingBadgeAwardGenericVoteEvents =
//        existingFollowSetsEvent.map(
//                followSetsEvent ->
//                    Filterable.getTypeSpecificTags(EventTag.class, followSetsEvent))
//            .orElse(List.of());
//
//    List<EventTag> nonMatchingBadgeAwardGenericVoteEvents =
//        Filterable.getTypeSpecificTagsStream(EventTag.class, materializedIncomingFollowSetsEvent)
//            .filter(incomingBadgeAwardVoteEvent ->
//                !existingBadgeAwardGenericVoteEvents.contains(incomingBadgeAwardVoteEvent)).toList();
//
//    IdentifierTag identifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, materializedIncomingFollowSetsEvent)
//        .stream()
//        .findFirst().orElseThrow();
//
//    FollowSetsEvent notifierFollowSetsEvent = createFollowSetsEvent(
//        voteReceiverPubkey,
//        identifierTag,
//        Stream.concat(
//            existingBadgeAwardGenericVoteEvents.stream(),
//            nonMatchingBadgeAwardGenericVoteEvents.stream()).toList());
//
////    TODO: below explicit save circumvents current issue w/ eventPlugin failing (due to Event remote fetch issue),  needs revisit
//    cacheServiceIF.save(notifierFollowSetsEvent);
////    TODO: since above is saved, below should now find event locally- then publish it
//    super.processIncomingEvent(notifierFollowSetsEvent);
//
//    FollowSetsEvent followsSetAsReputationEvent = createFollowSetsEvent(
//        voteReceiverPubkey,
//        identifierTag,
//        nonMatchingBadgeAwardGenericVoteEvents);
//    
//    return reputationEventPlugin.processIncomingEvent(followsSetAsReputationEvent);
//  }
//
//  private FollowSetsEvent createFollowSetsEvent(
//      @NonNull PublicKey voteReceiverPubkey,
//      @NonNull IdentifierTag identifierTag,
//      @NonNull List<EventTag> badgeAwardGenericVoteEvents) {
//    return new FollowSetsEvent(
//        aImgIdentity,
//        voteReceiverPubkey,
//        identifierTag,
//        relay,
//        DynamicReputationCalculator.class.getSimpleName(),
//        Collections.unmodifiableList(badgeAwardGenericVoteEvents));
//  }
//
//  private void deletePreviousFollowSetsEvent(FollowSetsEvent previousFollowSetsEvent) {
//    cacheServiceIF.deleteEvent(
//        new DeletionEvent(
//            aImgIdentity,
//            List.of(new EventTag(
//                previousFollowSetsEvent.getId(),
//                previousFollowSetsEvent.getRelayTagRelay().getUrl())), "aImg delete previous FOLLOW_SETS event"));
//  }
//
//  @Override
//  public Kind getKind() {
//    log.debug("getKind Kind[{}]: {}",
//        Kind.FOLLOW_SETS.getValue(),
//        Kind.FOLLOW_SETS.getName().toUpperCase());
//    return Kind.FOLLOW_SETS;
//  }
//}
