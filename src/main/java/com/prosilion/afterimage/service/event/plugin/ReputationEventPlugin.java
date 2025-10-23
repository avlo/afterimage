package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.afterimage.service.reputation.ReputationCalculationServiceIF;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKind;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindPlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import com.prosilion.superconductor.lib.redis.entity.EventNosqlEntityIF;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class ReputationEventPlugin extends PublishingEventKindPlugin {
  private final ReputationCalculationServiceIF reputationCalculationServiceIF;
  private final RedisCacheServiceIF redisCacheServiceIF;
  private final Identity aImgIdentity;

  public ReputationEventPlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindPluginIF eventKindTypePlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF) {
    super(notifierService, eventKindTypePlugin);
    this.redisCacheServiceIF = redisCacheServiceIF;
    this.reputationCalculationServiceIF = reputationCalculationServiceIF;
    this.aImgIdentity = aImgIdentity;
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF incomingReputationEvent) throws NostrException {
    PublicKey voteReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, incomingReputationEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

    IdentifierTag identifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, incomingReputationEvent)
        .stream()
        .findFirst().orElseThrow();

    getExistingBadgeAwardReputationEvent(
        voteReceiverPubkey,
        incomingReputationEvent.getPublicKey(),
//    TODO: ifPresent likely superfluous if delete mechanism already handles optional        
        identifierTag).ifPresent(this::deletePreviousBadgeAwardReputationEvent);

    BadgeDefinitionReputationEvent existingReputationDefinitionEvent = getReputationDefinitionEvent(
        incomingReputationEvent.getPublicKey(),
        identifierTag);

    BadgeAwardReputationEvent badgeAwardReputationEvent = createBadgeAwardReputationEvent(voteReceiverPubkey,
        existingReputationDefinitionEvent);

    super.processIncomingEvent(
        reputationCalculationServiceIF.calculateReputationEvent(
            voteReceiverPubkey,
            badgeAwardReputationEvent,
            incomingReputationEvent));
  }

  private BadgeDefinitionReputationEvent getReputationDefinitionEvent(PublicKey eventCreatorPubkey, IdentifierTag uuid) {
    EventNosqlEntityIF nakedDefinitionEvent =
        redisCacheServiceIF.getEventsByKindAndAuthorPublicKeyAndIdentifierTag(
                Kind.BADGE_DEFINITION_EVENT,
                eventCreatorPubkey,
                uuid)
            .stream()
            .max(Comparator.comparing(EventIF::getCreatedAt))
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Redis BadgeDefinitionReputationEvent matching PublicKey [%s] with Uuid [%s] not found",
                    eventCreatorPubkey, uuid)));

    List<FormulaEvent> formulaEvents =
        Filterable.getTypeSpecificTagsStream(EventTag.class, nakedDefinitionEvent)
            .map(eventTag -> redisCacheServiceIF
                .getEventByEventId(eventTag.getIdEvent()).orElseThrow(
                    () -> new NoSuchElementException(
                        String.format("Redis FormulaEvent matching EventID [%s] was found",
                            eventTag.getIdEvent()))
                ))
            .filter(event ->
                event.getKind().equals(Kind.ARBITRARY_CUSTOM_APP_DATA))
            .map(this::asFormulaEvent)
            .toList();

    BadgeDefinitionReputationEvent foundReputationDefinitionEvent = new BadgeDefinitionReputationEvent(
        aImgIdentity,
        uuid,
        formulaEvents);
    return foundReputationDefinitionEvent;
  }

  private Optional<GenericEventKind> getExistingBadgeAwardReputationEvent(
      PublicKey badgeReceiverPubkey,
      PublicKey eventCreatorPubkey,
      IdentifierTag uuid) {
    Optional<GenericEventKind> badgeAwardReputationEvent = redisCacheServiceIF
        .getEventsByKindAndPubKeyTagAndAddressTag(
            Kind.BADGE_AWARD_EVENT,
            badgeReceiverPubkey,
            new AddressTag(
                Kind.BADGE_DEFINITION_EVENT,
                eventCreatorPubkey,
                uuid))
        .stream()
        .max(Comparator.comparing(EventIF::getCreatedAt))
        .map(ReputationEventPlugin::createGenericEventKind);
    return badgeAwardReputationEvent;
  }

  private BadgeAwardReputationEvent createBadgeAwardReputationEvent(
      PublicKey badgeReceiverPubkey,
      BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) {
    BadgeAwardReputationEvent badgeAwardReputationEvent = new BadgeAwardReputationEvent(
        aImgIdentity,
        badgeReceiverPubkey,
        badgeDefinitionReputationEvent,
        BigDecimal.ZERO);
    return badgeAwardReputationEvent;
  }

  private void deletePreviousBadgeAwardReputationEvent(EventIF previousReputationEvent) {
    redisCacheServiceIF.deleteEvent(
        new DeletionEvent(
            aImgIdentity,
            List.of(new EventTag(previousReputationEvent.getId())), "aImg delete previous REPUTATION event"));
  }

  @Override
  public Kind getKind() {
    log.debug("{} getKind returning {}}", getClass().getSimpleName(), super.getKind());
    return super.getKind();
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
  private FormulaEvent asFormulaEvent(EventNosqlEntityIF formulaEvent) {
    return new FormulaEvent(
        aImgIdentity,
        Filterable.getTypeSpecificTagsStream(IdentifierTag.class, formulaEvent)
            .findFirst().orElseThrow(),
        formulaEvent.getContent());
  }
}
