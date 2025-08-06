package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.service.GenericEventKind;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindType;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindTypePlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import com.prosilion.superconductor.lib.redis.dto.GenericDocumentKindTypeDto;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
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
  public void processIncomingEvent(@NonNull EventIF voteEvent) {
    PublicKey voteReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, voteEvent).stream()
        .map(PubKeyTag::getPublicKey).findFirst().orElseThrow();

    GenericEventKindTypeIF calculatedReputationEvent = calculateReputationEvent(voteReceiverPubkey, voteEvent.getContent());

    deletePreviousReputationCalculationEvent(voteReceiverPubkey);

    super.processIncomingEvent(calculatedReputationEvent);
  }

  private GenericEventKindTypeIF calculateReputationEvent(PublicKey voteReceiverPubkey, String voteValue) throws NostrException, NoSuchAlgorithmException {
    return createReputationEvent(
        voteReceiverPubkey,
        new BigDecimal(voteValue)
            .add(
                getAllVoteEventsForCalculation(voteReceiverPubkey)
                    .map(
                        GenericEventKindTypeIF::getContent)
                    .map(
                        BigDecimal::new).orElse(BigDecimal.ZERO)));
  }

  private void deletePreviousReputationCalculationEvent(PublicKey badgeReceiverPubkey) throws NostrException, NoSuchAlgorithmException {
    cacheServiceIF.deleteEventEntity(
        new DeletionEvent(
            aImgIdentity,
            getAllPubkeyReputationEvents(badgeReceiverPubkey).stream()
                .map(GenericEventKindType::getId)
                .map(EventTag::new).toList(), "aImg deletion event"));
  }

  public Optional<GenericEventKindType> getAllVoteEventsForCalculation(PublicKey badgeReceiverPubkey) {
    String notKindType = AfterimageKindType.REPUTATION.getName();

    return cacheServiceIF
        .getByKind(Kind.BADGE_AWARD_EVENT).stream().map(e ->
            cacheServiceIF.getEventByEventId(e.getId()))
        .filter(eventIF -> eventIF.orElseThrow().getTags()
            .stream()
            .filter(PubKeyTag.class::isInstance)
            .map(PubKeyTag.class::cast)
            .anyMatch(pubKeyTag -> pubKeyTag.getPublicKey().equals(badgeReceiverPubkey)))
        .filter(eventIF -> eventIF.orElseThrow().getTags()
            .stream()
            .filter(AddressTag.class::isInstance)
            .map(AddressTag.class::cast)
            .anyMatch(addressTag ->
                Optional.ofNullable(
                        addressTag.getIdentifierTag()).orElseThrow(() ->
                        new InvalidTagException("NULL", List.of(notKindType)))
                    .getUuid().equals(notKindType)))
        .flatMap(Optional::stream)
        .max(Comparator.comparing(EventIF::getCreatedAt))
        .map(eventIF ->
            new GenericEventKindType(
                new GenericEventKind(
                    eventIF.getId(),
                    aImgIdentity.getPublicKey(),
                    eventIF.getCreatedAt(),
                    eventIF.getKind(),
                    eventIF.getTags(),
                    eventIF.getContent(),
                    eventIF.getSignature()),
                getKindType()));
  }

  public List<GenericEventKindType> getAllPubkeyReputationEvents(PublicKey badgeReceiverPubkey) {

    return cacheServiceIF
        .getAll().stream()
        .filter(eventIF -> eventIF.getKind().equals(Kind.BADGE_AWARD_EVENT))
        .filter(eventIF -> eventIF.getTags()
            .stream()
            .filter(PubKeyTag.class::isInstance)
            .map(PubKeyTag.class::cast)
            .anyMatch(pubKeyTag -> pubKeyTag.getPublicKey().equals(badgeReceiverPubkey)))
        .filter(eventIF -> eventIF.getTags()
            .stream()
            .filter(AddressTag.class::isInstance)
            .map(AddressTag.class::cast)
            .anyMatch(addressTag ->
                Optional.ofNullable(
                        addressTag.getIdentifierTag()).orElseThrow(() ->
                        new InvalidTagException("NULL", List.of(getKindType().getName())))
                    .getUuid().equals(AfterimageKindType.REPUTATION.getName())))
        .map(genericEventKind ->
            new GenericEventKindType(
                new GenericEventKind(
                    genericEventKind.getId(),
                    genericEventKind.getPublicKey(),
                    genericEventKind.getCreatedAt(),
                    genericEventKind.getKind(),
                    genericEventKind.getTags(),
                    genericEventKind.getContent(),
                    genericEventKind.getSignature()),
                AfterimageKindType.REPUTATION)).toList();
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
