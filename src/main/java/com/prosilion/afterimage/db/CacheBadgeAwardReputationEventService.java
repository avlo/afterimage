package com.prosilion.afterimage.db;

import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.EventTagsMappedEventsIF;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.autoconfigure.base.service.AbstractCacheEventTagBaseEventService;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;

public class CacheBadgeAwardReputationEventService extends AbstractCacheEventTagBaseEventService {
  public CacheBadgeAwardReputationEventService(@NonNull @Qualifier("redisCacheService") CacheServiceIF cacheServiceIF) {
    super(cacheServiceIF);
  }

  public BadgeAwardReputationEvent populate(
      @NonNull GenericEventRecord baseEvent,
      @NonNull List<GenericEventRecord> unpopulatedEvents) {

    Function<EventTag, BadgeDefinitionReputationEvent> fxn = eventTag ->
        createEventTagMappedEvent(unpopulatedEvents.stream().filter(genericEventRecord ->
                genericEventRecord.getId().equals(eventTag.getIdEvent()))
            .findFirst().orElseThrow());

    return createEventGivenMappedEventTagEvents(
        baseEvent,
        BadgeAwardReputationEvent.class,
        fxn);
  }

  private BadgeDefinitionReputationEvent createEventTagMappedEvent(GenericEventRecord genericEventRecord) {
    return super.createBaseEvent(
        genericEventRecord,
        BadgeDefinitionReputationEvent.class);
  }

  public Optional<? extends EventTagsMappedEventsIF> getBadgeAwardReputationEvent(
      @NonNull PublicKey badgeReceiverPubkey,
      @NonNull PublicKey eventCreatorPubkey,
      @NonNull IdentifierTag uuid) {
    List<? extends EventTagsMappedEventsIF> eventsByKindAndPubKeyTagAndAddressTag = super.getEventsByKindAndPubKeyTagAndAddressTag(
        Kind.BADGE_AWARD_EVENT,
        badgeReceiverPubkey,
        new AddressTag(
            Kind.BADGE_DEFINITION_EVENT,
            eventCreatorPubkey, uuid));

    EventTagsMappedEventsIF first1 = eventsByKindAndPubKeyTagAndAddressTag.getFirst();
    Optional<EventTagsMappedEventsIF> first2 = Optional.ofNullable(first1);
    return first2;
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }
}
