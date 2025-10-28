package com.prosilion.afterimage.db;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKind;
import com.prosilion.superconductor.lib.redis.entity.EventNosqlEntityIF;
import com.prosilion.superconductor.lib.redis.service.DeletionEventNoSqlEntityService;
import com.prosilion.superconductor.lib.redis.service.EventNosqlEntityService;
import com.prosilion.superconductor.lib.redis.service.RedisCacheService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.springframework.lang.NonNull;

public class AfterimageCacheService extends RedisCacheService {
  private final Identity aImgIdentity;

  public AfterimageCacheService(
      @NonNull EventNosqlEntityService eventNosqlEntityService,
      @NonNull DeletionEventNoSqlEntityService deletionEventNoSqlEntityService,
      @NonNull Identity aImgIdentity) {
    super(eventNosqlEntityService, deletionEventNoSqlEntityService);
    this.aImgIdentity = aImgIdentity;
  }

  public EventNosqlEntityIF saveWithEvents(@NonNull EventIF event) {
//    TODO: if eventTags are included, consider including eventTagEvents existence using nostr REQ  
    List<EventIF> formulaEvents = Stream.of(event)
        .filter(BadgeDefinitionReputationEvent.class::isInstance)
        .map(BadgeDefinitionReputationEvent.class::cast)
        .map(BadgeDefinitionReputationEvent::getFormulaEvents)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    return saveWithEventTags(event, formulaEvents);
  }

  public Optional<GenericEventKind> getBadgeAwardReputationEvent(
      PublicKey badgeReceiverPubkey,
      PublicKey eventCreatorPubkey,
      IdentifierTag uuid) {
    Optional<GenericEventKind> dbBadgeAwardReputationEntity = super
        .getEventsByKindAndPubKeyTagAndAddressTag(
            Kind.BADGE_AWARD_EVENT,
            badgeReceiverPubkey,
            new AddressTag(
                Kind.BADGE_DEFINITION_EVENT,
                eventCreatorPubkey,
                uuid))
        .stream()
        .max(Comparator.comparing(EventIF::getCreatedAt))
        .map(AfterimageCacheService::createGenericEventKind);
    return dbBadgeAwardReputationEntity;
  }

  public BadgeDefinitionReputationEvent getBadgeDefinitionReputationEvent(
      @NonNull PublicKey eventCreatorPubkey,
      @NonNull IdentifierTag identifierTag) {
    List<EventTag> eventTagStream = super.getEventsByKindAndAuthorPublicKeyAndIdentifierTag(
            Kind.BADGE_DEFINITION_EVENT,
            eventCreatorPubkey,
            identifierTag)
        .stream()
        .flatMap(entity ->
            Filterable.getTypeSpecificTagsStream(EventTag.class, entity))
        .toList();

    List<String> eventIds = eventTagStream.stream()
        .map(EventTag::getIdEvent).toList();

    List<EventNosqlEntityIF> dbEvents = eventIds.stream()
        .map(super::getEventByEventId)
        .flatMap(Optional::stream).toList();

    List<EventNosqlEntityIF> matchingArbitraryAppKinds = dbEvents.stream()
        .filter(event ->
            event.getKind().equals(Kind.ARBITRARY_CUSTOM_APP_DATA))
        .toList();

    List<FormulaEvent> formulaEvents = asFormulaEvents(matchingArbitraryAppKinds);

    BadgeDefinitionReputationEvent badgeDefinitionReputationEvent = new BadgeDefinitionReputationEvent(
        aImgIdentity,
        identifierTag,
        formulaEvents);
    return badgeDefinitionReputationEvent;
  }

  private static GenericEventKind createGenericEventKind(EventNosqlEntityIF eventIF) {
    return new GenericEventKind(
        eventIF.getId(),
        eventIF.getPublicKey(),
        eventIF.getCreatedAt(),
        eventIF.getKind(),
        eventIF.getTags(),
        eventIF.getContent(),
        eventIF.getSignature());
  }

  @SneakyThrows
  private List<FormulaEvent> asFormulaEvents(List<EventNosqlEntityIF> formulaEntities) {
    ArrayList<FormulaEvent> result = new ArrayList<>();
    for (EventNosqlEntityIF formulaEntity : formulaEntities) {
      result.add(
          new FormulaEvent(
              aImgIdentity,
              new BadgeDefinitionAwardEvent(aImgIdentity,
                  Filterable.getTypeSpecificTagsStream(IdentifierTag.class, formulaEntity)
                      .findFirst().orElseThrow()),
              formulaEntity.getContent()));
    }
    return result;
  }
}
