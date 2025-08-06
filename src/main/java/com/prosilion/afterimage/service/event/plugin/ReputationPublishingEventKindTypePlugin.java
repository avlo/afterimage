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
    GenericEventKindTypeIF calculatedReputationEvent = calculateReputationEvent(voteEvent);
    deletePreviousReputationEventShouldBeSingularEvent(calculatedReputationEvent);
    super.processIncomingEvent(calculatedReputationEvent);
  }

  private GenericEventKindTypeIF calculateReputationEvent(EventIF incomingVoteEventByUser) throws NostrException, NoSuchAlgorithmException {
    PublicKey badgeReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, incomingVoteEventByUser).stream()
        .map(PubKeyTag::getPublicKey).findFirst().orElseThrow();

    Optional<GenericEventKindType> existingVotes = getAllVoteEventsForCalculation(badgeReceiverPubkey);

    BigDecimal existingRep = existingVotes
        .map(GenericEventKindTypeIF::getContent)
        .map(BigDecimal::new).orElse(BigDecimal.ZERO);

    BigDecimal updatedRep = new BigDecimal(incomingVoteEventByUser.getContent()).add(existingRep);

    GenericEventKindTypeIF reputationEvent = createReputationEvent(
        badgeReceiverPubkey,
        updatedRep);

    return reputationEvent;
  }

  private void deletePreviousReputationEventShouldBeSingularEvent(EventIF event) throws NostrException, NoSuchAlgorithmException {
    PublicKey badgeReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, event).stream()
        .map(PubKeyTag::getPublicKey).findFirst().orElseThrow();

    List<GenericEventKindType> existingReputation = getAllPubkeyReputationEvents(badgeReceiverPubkey);
    List<EventTag> list = existingReputation.stream().map(GenericEventKindType::getId).map(EventTag::new).toList();

    DeletionEvent secondDeletionEvent = new DeletionEvent(aImgIdentity, list, "aImg deletion event");
    cacheServiceIF.deleteEventEntity(secondDeletionEvent);

//    existingReputation.forEach(cacheServiceIF::deleteEventEntity);
  }

  public Optional<GenericEventKindType> getAllVoteEventsForCalculation(PublicKey badgeReceiverPubkey) {
    String notKindType = AfterimageKindType.REPUTATION.getName();

    return cacheServiceIF
        .getByKind(Kind.BADGE_AWARD_EVENT).stream().map(e ->
            cacheServiceIF.getEventByEventId(e.getId()))
        .filter(genericEventKind1 -> genericEventKind1.orElseThrow().getTags()
            .stream()
            .filter(PubKeyTag.class::isInstance)
            .map(PubKeyTag.class::cast)
            .anyMatch(pubKeyTag -> pubKeyTag.getPublicKey().equals(badgeReceiverPubkey)))
        .filter(genericEventKind1 -> genericEventKind1.orElseThrow().getTags()
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
        .map(genericEventKind ->
            new GenericEventKindType(
                new GenericEventKind(
                    genericEventKind.getId(),
                    aImgIdentity.getPublicKey(),
                    genericEventKind.getCreatedAt(),
                    genericEventKind.getKind(),
                    genericEventKind.getTags(),
                    genericEventKind.getContent(),
                    genericEventKind.getSignature()),
                getKindType()));
  }

  public List<GenericEventKindType> getAllPubkeyReputationEvents(PublicKey badgeReceiverPubkey) {
    List<? extends EventIF> all = cacheServiceIF
        .getAll();

    List<? extends EventIF> allBadgeAwardEvents = all.stream()
        .filter(eventIF -> eventIF.getKind().equals(Kind.BADGE_AWARD_EVENT)).toList();

    List<? extends EventIF> allBadgeAwardEventsMatchingUpvotedUser = allBadgeAwardEvents.stream()
        .filter(genericEventKind -> genericEventKind.getTags()
            .stream()
            .filter(PubKeyTag.class::isInstance)
            .map(PubKeyTag.class::cast)
            .anyMatch(pubKeyTag -> pubKeyTag.getPublicKey().equals(badgeReceiverPubkey))).toList();

    List<? extends EventIF> allVotes = allBadgeAwardEventsMatchingUpvotedUser.stream()
        .filter(genericEventKind -> genericEventKind.getTags()
            .stream()
            .filter(AddressTag.class::isInstance)
            .map(AddressTag.class::cast)
            .anyMatch(addressTag ->
                Optional.ofNullable(
                        addressTag.getIdentifierTag()).orElseThrow(() ->
                        new InvalidTagException("NULL", List.of(getKindType().getName())))
                    .getUuid().equals(AfterimageKindType.REPUTATION.getName()))).toList();

    List<GenericEventKindType> genericEventKindTypes = allVotes.stream()
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
    return genericEventKindTypes;
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
