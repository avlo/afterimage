package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.service.GenericEventKind;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindIF;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindType;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindTypePlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import com.prosilion.superconductor.lib.redis.dto.GenericDocumentKindTypeDto;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class ReputationPublishingEventKindTypePlugin extends PublishingEventKindTypePlugin {
  private final CacheServiceIF cacheServiceIF;
  private final Identity aImgIdentity;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;

  public ReputationPublishingEventKindTypePlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindTypePluginIF<KindTypeIF> eventKindTypePlugin,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    super(notifierService, eventKindTypePlugin);
    this.cacheServiceIF = cacheServiceIF;
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.aImgIdentity = aImgIdentity;
  }

  @SneakyThrows
  public void processIncomingEvent(@NonNull GenericEventKindIF voteEvent) {
    GenericEventKindTypeIF calculatedReputationEvent = calculateReputationEvent(voteEvent);
    deletePreviousReputationEventShouldBeSingularEvent(voteEvent);
    super.processIncomingEvent(calculatedReputationEvent);
  }

  private void deletePreviousReputationEventShouldBeSingularEvent(GenericEventKindIF event) throws NostrException {
    PublicKey badgeReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, event).stream()
        .map(PubKeyTag::getPublicKey).findFirst().orElseThrow();

    List<GenericEventKindType> existingReputation = getAllEventsByKindType(badgeReceiverPubkey, AfterimageKindType.REPUTATION);
    existingReputation.forEach(cacheServiceIF::deleteEventEntity);
  }

  private GenericEventKindTypeIF calculateReputationEvent(GenericEventKindIF event) throws NostrException, NoSuchAlgorithmException {
    PublicKey badgeReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, event).stream()
        .map(PubKeyTag::getPublicKey).findFirst().orElseThrow();

    List<GenericEventKindType> existingVotes = getAllEventsByKindType(badgeReceiverPubkey, getKindType());

    return createReputationEvent(
        badgeReceiverPubkey,
        existingVotes.stream()
            .map(GenericEventKindTypeIF::getContent)
            .map(BigDecimal::new).toList().stream()
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO));
  }

  private List<GenericEventKindType> getAllEventsByKindType(PublicKey badgeReceiverPubkey, KindTypeIF kindTypeIF) {
    return cacheServiceIF
        .getByKind(Kind.BADGE_AWARD_EVENT).stream().map(e ->
            cacheServiceIF.getEventByEventId(e.getId()))
        .filter(genericEventKind -> genericEventKind.orElseThrow().getTags()
            .stream()
            .filter(PubKeyTag.class::isInstance)
            .map(PubKeyTag.class::cast)
            .anyMatch(pubKeyTag -> pubKeyTag.getPublicKey().equals(badgeReceiverPubkey)))
        .filter(genericEventKind -> genericEventKind.orElseThrow().getTags()
            .stream()
            .filter(AddressTag.class::isInstance)
            .map(AddressTag.class::cast)
            .anyMatch(addressTag ->
                !Optional.ofNullable(
                        addressTag.getIdentifierTag()).orElseThrow(() ->
                        new InvalidTagException("NULL", List.of(getKindType().getName())))
                    .getUuid().equals(kindTypeIF.getName())))
        .map(genericEventKind ->
            new GenericEventKindType(
                new GenericEventKind(
                    genericEventKind.orElseThrow().getId(),
                    genericEventKind.orElseThrow().getPublicKey(),
                    genericEventKind.orElseThrow().getCreatedAt(),
                    genericEventKind.orElseThrow().getKind(),
                    genericEventKind.orElseThrow().getTags(),
                    genericEventKind.orElseThrow().getContent(),
                    genericEventKind.orElseThrow().getSignature()),
                kindTypeIF)).toList();
  }

  private GenericEventKindTypeIF createReputationEvent(@NonNull PublicKey badgeReceiverPubkey, @NonNull BigDecimal score) throws NostrException, NoSuchAlgorithmException {
    return new GenericDocumentKindTypeDto(
        new BadgeAwardReputationEvent(
            aImgIdentity,
            badgeReceiverPubkey,
            reputationBadgeDefinitionEvent,
            score),
        AfterimageKindType.REPUTATION).convertBaseEventToGenericEventKindTypeIF();
  }

  @Override
  public Kind getKind() {
    log.debug("ReputationEventKindTypePlugin getKind returning Kind.BADGE_AWARD_EVENT");
    return super.getKind();
  }

  @Override
  public KindTypeIF getKindType() {
    log.debug("ReputationEventKindTypePlugin getKindType returning Kind.REPUTATION");
    return super.getKindType();
  }
}
