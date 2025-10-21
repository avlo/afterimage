package com.prosilion.afterimage.db;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.lib.redis.service.DeletionEventNoSqlEntityService;
import com.prosilion.superconductor.lib.redis.service.EventNosqlEntityService;
import com.prosilion.superconductor.lib.redis.service.RedisCacheService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;

public class AfterimageCacheService extends RedisCacheService {

  public AfterimageCacheService(
      @NonNull EventNosqlEntityService eventNosqlEntityService,
      @NonNull DeletionEventNoSqlEntityService deletionEventNoSqlEntityService) {
    super(eventNosqlEntityService, deletionEventNoSqlEntityService);
  }

  public Optional<BadgeDefinitionReputationEvent> getBadgeDefinitionReputationEvent(
      @NonNull PublicKey publicKey,
      @NonNull IdentifierTag identifierTag) {
    return Optional.of((BadgeDefinitionReputationEvent)
        super.getEventsByKindAndIdentifierTag(
                Kind.BADGE_DEFINITION_EVENT,
                identifierTag)
            .stream().filter(event ->
                event.getPublicKey().equals(publicKey)).toList().getFirst());
  }

  Map<String, String> getIdentiferTagFormulaMap(@NonNull BadgeDefinitionReputationEvent event) {
    if (event.getEventTags().isEmpty()) 
      return new HashMap<>();

    if (event.getEventTags().stream()
        .map(eventTag ->
            super.getEventByEventId(eventTag.getIdEvent())).toList().isEmpty())
      return new HashMap<>();

    return event.getEventTags().stream()
        .map(eventTag ->
            super.getEventByEventId(eventTag.getIdEvent()))
            .map(FormulaEvent.class::cast)
            .collect(
                Collectors.toMap(item ->
                        item.getIdentifierTag().getUuid(),
                    FormulaEvent::getFormula,
                    (prev, next) -> next, HashMap::new));
  }
}
