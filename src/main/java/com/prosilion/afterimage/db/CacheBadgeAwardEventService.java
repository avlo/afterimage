package com.prosilion.afterimage.db;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardAbstractEvent;
import com.prosilion.nostr.event.EventTagsMappedEventsIF;
import com.prosilion.nostr.event.FormulaEvent;
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
import org.springframework.lang.NonNull;

public class CacheBadgeAwardEventService extends AbstractCacheEventTagBaseEventService {
  public CacheBadgeAwardEventService(@NonNull CacheServiceIF cacheServiceIF) {
    super(cacheServiceIF);
  }

  public EventTagsMappedEventsIF populate(
      @NonNull GenericEventRecord baseEvent,
      @NonNull List<GenericEventRecord> unpopulatedEvents) {

    Function<EventTag, BadgeAwardAbstractEvent> fxn = eventTag ->
        createEventTagMappedEvent(unpopulatedEvents.stream().filter(genericEventRecord ->
                genericEventRecord.getId().equals(eventTag.getIdEvent()))
            .findFirst().orElseThrow());

    return createEventGivenMappedEventTagEvents(
        baseEvent,
        FormulaEvent.class,
        fxn);
  }

  private BadgeAwardAbstractEvent createEventTagMappedEvent(GenericEventRecord genericEventRecord) {
    return super.createBaseEvent(
        genericEventRecord,
        BadgeAwardAbstractEvent.class);
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
