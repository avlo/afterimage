package com.prosilion.afterimage.config;

import com.prosilion.afterimage.db.AfterimageCacheService;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.FormulaEvent;
import java.util.Collections;
import org.springframework.lang.NonNull;

public class DataLoaderRedis implements DataLoaderRedisIF {
  private final AfterimageCacheService afterimageCacheService;
  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEvent;

  public DataLoaderRedis(
      @NonNull AfterimageCacheService afterimageCacheService,
      @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) {
    this.afterimageCacheService = afterimageCacheService;
    this.badgeDefinitionReputationEvent = badgeDefinitionReputationEvent;
  }

  @Override
  public void run(String... args) {
    afterimageCacheService.saveWithEvents(badgeDefinitionReputationEvent);
    System.out.println("0000000000000000000000000");
    System.out.println("0000000000000000000000000");
    System.out.printf("badgeDefinitionReputationEvent eventId: [%s]%n", badgeDefinitionReputationEvent.getId());
    System.out.printf("badgeDefinitionReputationEvent author PubKey: [%s]%n", badgeDefinitionReputationEvent.getPublicKey());
    System.out.println("-------------------------");
    badgeDefinitionReputationEvent.getEventTags().forEach(eventTag ->
        System.out.printf("eventTag.getIdEvent: [%s]%n", eventTag.getIdEvent()));
    System.out.println("-------------------------");
    badgeDefinitionReputationEvent.getFormulaEvents().stream().map(FormulaEvent::getId).forEach(formulaEventId ->
        System.out.printf("FormulaEvent::getId: [%s]%n", formulaEventId));
    System.out.println("0000000000000000000000000");
    System.out.println("0000000000000000000000000");
  }
}
