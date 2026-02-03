//package com.prosilion.afterimage.db;
//
//import com.prosilion.nostr.enums.Kind;
//import com.prosilion.nostr.event.BadgeAwardReputationEvent;
//import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
//import com.prosilion.nostr.event.DeletionEvent;
//import com.prosilion.nostr.event.EventIF;
//import com.prosilion.nostr.tag.IdentifierTag;
//import com.prosilion.nostr.user.PublicKey;
//import com.prosilion.superconductor.autoconfigure.base.service.CacheBadgeAwardReputationEventService;
//import com.prosilion.superconductor.autoconfigure.base.service.CacheBadgeDefinitionReputationEventService;
//import java.util.List;
//import java.util.Optional;
//import org.springframework.lang.NonNull;
//
//public class AfterimageCacheService {
//  private final CacheBadgeDefinitionReputationEventService defnReputationEventService;
//  private final CacheBadgeAwardReputationEventService cacheBadgeAwardEventService;
//
//  public AfterimageCacheService(
//      @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
//      @NonNull CacheBadgeAwardReputationEventService cacheBadgeAwardEventService) {
//    this.defnReputationEventService = cacheBadgeDefinitionReputationEventService;
//    this.cacheBadgeAwardEventService = cacheBadgeAwardEventService;
//  }
//
//  public void save(@NonNull BadgeDefinitionReputationEvent event) {
//    defnReputationEventService.save(event);
//  }
//
//  public Optional<BadgeAwardReputationEvent> getBadgeAwardReputationEvent(
//      @NonNull PublicKey badgeReceiverPubkey,
//      @NonNull PublicKey eventCreatorPubkey,
//      @NonNull IdentifierTag uuid) {
//    Optional<? extends EventTagsMappedEventsIF> badgeAwardReputationEvent = cacheBadgeAwardEventService.(
//        badgeReceiverPubkey,
//        eventCreatorPubkey,
//        uuid);
//    return (Optional<BadgeAwardReputationEvent>) badgeAwardReputationEvent;
//  }
//
//  public BadgeDefinitionReputationEvent getBadgeDefinitionReputationEvent(
//      @NonNull PublicKey eventCreatorPubkey,
//      @NonNull IdentifierTag identifierTag) {
//    List<BadgeDefinitionReputationEvent> badgeDefinitionReputationEvents = defnReputationEventService.getBadgeDefinitionReputationEvents(
//        Kind.BADGE_DEFINITION_EVENT,
//        eventCreatorPubkey,
//        identifierTag);
//
//    return badgeDefinitionReputationEvents.getFirst();
//  }
//
//  public void deleteEvent(@NonNull DeletionEvent deletionEvent) {
//    cacheBadgeAwardEventService.deleteEvent(deletionEvent);
//  }
//}
