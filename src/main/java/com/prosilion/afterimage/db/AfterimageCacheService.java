package com.prosilion.afterimage.db;

import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.EventTagsMappedEventsIF;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.autoconfigure.base.service.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.base.service.CacheEventTagBaseEventServiceIF;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;

public class AfterimageCacheService {
  private final CacheBadgeDefinitionReputationEventService defnReputationEventService;
  private final CacheBadgeAwardEventService cacheBadgeAwardEventService;

  public AfterimageCacheService(
      @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
      @NonNull CacheEventTagBaseEventServiceIF cacheBadgeAwardEventService) {
    this.defnReputationEventService = cacheBadgeDefinitionReputationEventService;
    this.cacheBadgeAwardEventService = (CacheBadgeAwardEventService) cacheBadgeAwardEventService;
  }

  public void save(@NonNull EventIF event) {
    defnReputationEventService.save(event);
  }

  public Optional<BadgeAwardReputationEvent> getBadgeAwardReputationEvent(
      @NonNull PublicKey badgeReceiverPubkey,
      @NonNull PublicKey eventCreatorPubkey,
      @NonNull IdentifierTag uuid) {
    Optional<? extends EventTagsMappedEventsIF> badgeAwardReputationEvent = cacheBadgeAwardEventService.getBadgeAwardReputationEvent(
        badgeReceiverPubkey,
        eventCreatorPubkey,
        uuid);
    return (Optional<BadgeAwardReputationEvent>) badgeAwardReputationEvent;
  }

  public BadgeDefinitionReputationEvent getBadgeDefinitionReputationEvent(
      @NonNull PublicKey eventCreatorPubkey,
      @NonNull IdentifierTag identifierTag) {
    List<BadgeDefinitionReputationEvent> badgeDefinitionReputationEvents = defnReputationEventService.getBadgeDefinitionReputationEvents(
        Kind.BADGE_DEFINITION_EVENT,
        eventCreatorPubkey,
        identifierTag);

    return badgeDefinitionReputationEvents.getFirst();
  }

  public void deleteEvent(@NonNull DeletionEvent deletionEvent) {
    cacheBadgeAwardEventService.deleteEvent(deletionEvent);
  }
}
