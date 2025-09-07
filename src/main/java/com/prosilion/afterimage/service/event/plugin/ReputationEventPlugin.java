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
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FollowSetsEvent.EventTagAddressTagPair;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKind;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindType;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindTypePlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import com.prosilion.superconductor.lib.redis.document.EventDocumentIF;
import com.prosilion.superconductor.lib.redis.dto.GenericDocumentKindTypeDto;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class ReputationEventPlugin extends PublishingEventKindTypePlugin {
  private final RedisCacheServiceIF redisCacheServiceIF;
  private final Identity aImgIdentity;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;

  public ReputationEventPlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindTypePluginIF eventKindTypePlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    super(notifierService, eventKindTypePlugin);
    this.redisCacheServiceIF = redisCacheServiceIF;
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.aImgIdentity = aImgIdentity;
  }

  @SneakyThrows
  public void processIncomingEvent(@NonNull EventIF incomingFollowSetsEvent) {
    PublicKey voteReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, incomingFollowSetsEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

//    REPUTATION:
    Optional<GenericEventKindType> previousReputationEvent = getPreviousReputationEvent(voteReceiverPubkey);
    previousReputationEvent.ifPresent(this::deletePreviousReputationCalculationEvent);

    BigDecimal previousScore = previousReputationEvent
        .map(GenericEventKindTypeIF::getContent)
        .map(BigDecimal::new)
        .orElse(BigDecimal.ZERO);

    GenericEventKindTypeIF updatedReputationEvent = calculateReputationEvent(
        voteReceiverPubkey,
        previousScore,
        incomingFollowSetsEvent.getContent());

    super.processIncomingEvent(updatedReputationEvent);

//    FOLLOW SETS:
    Optional<GenericEventKind> existingFollowSetsEvents = getPreviousFollowSetsEvent(voteReceiverPubkey);

    if (existingFollowSetsEvents.isEmpty()) {
      super.processIncomingEvent(incomingFollowSetsEvent);
      return;
    }

    List<EventTagAddressTagPair> incomingEventTagAddressTagPairs = getEventTagAddressTagPairs(incomingFollowSetsEvent.getTags());
    List<EventTagAddressTagPair> existingEventTagAddressTagPairs = getEventTagAddressTagPairs(existingFollowSetsEvents.orElseThrow().getTags());

    List<EventTagAddressTagPair> nonMatches = incomingEventTagAddressTagPairs.stream()
        .filter(incomingEventTagAddressTagPair ->
            !existingEventTagAddressTagPairs.contains(incomingEventTagAddressTagPair)).toList();

    EventIF updatedFollowSetsEvent = createFollowSetsEvent(
        voteReceiverPubkey,
        Stream.concat(existingEventTagAddressTagPairs.stream(), nonMatches.stream()).toList());

    deletePreviousReputationCalculationEvent(incomingFollowSetsEvent);
    super.processIncomingEvent(updatedFollowSetsEvent);
    log.debug("pause for debug");
  }

  private GenericEventKindTypeIF calculateReputationEvent(
      PublicKey voteReceiverPubkey,
      BigDecimal previousScore,
      String voteValue) throws NostrException, NoSuchAlgorithmException {

    return createReputationEvent(
        voteReceiverPubkey,
        new BigDecimal(voteValue).add(previousScore));
  }

  @SneakyThrows
  private void deletePreviousReputationCalculationEvent(GenericEventKindType previousReputationEvent) {
    redisCacheServiceIF.deleteEvent(
        new DeletionEvent(
            aImgIdentity,
            List.of(new EventTag(previousReputationEvent.getId())), "aImg delete previous REPUTATION event"));
  }

  public Optional<GenericEventKindType> getPreviousReputationEvent(PublicKey badgeReceiverPubkey) {
    List<EventDocumentIF> eventsByKindAndPubKeyTag = redisCacheServiceIF
        .getEventsByKindAndPubKeyTag(Kind.BADGE_AWARD_EVENT, badgeReceiverPubkey);

    return eventsByKindAndPubKeyTag
        .stream()
        .filter(eventIF -> eventIF.getTags()
            .stream()
            .filter(AddressTag.class::isInstance)
            .map(AddressTag.class::cast)
            .anyMatch(addressTag ->
                Optional.ofNullable(
                        addressTag.getIdentifierTag()).orElseThrow(() ->
                        new InvalidTagException("NULL", List.of(getKindType().getName())))
                    .getUuid().equals(getKindType().getName())))
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

  public Optional<GenericEventKindType> getAllPubkeyReputationEvents(@NonNull PublicKey badgeReceiverPubkey) {
    return getPreviousReputationEvent(badgeReceiverPubkey);
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

  private Optional<GenericEventKind> getPreviousFollowSetsEvent(PublicKey badgeReceiverPubkey) {
    return redisCacheServiceIF
        .getEventsByKindAndPubKeyTag(Kind.FOLLOW_SETS, badgeReceiverPubkey)
        .stream()
        .max(Comparator.comparing(EventIF::getCreatedAt))
        .map(eventIF ->
            new GenericEventKind(
                eventIF.getId(),
                eventIF.getPublicKey(),
                eventIF.getCreatedAt(),
                eventIF.getKind(),
                eventIF.getTags(),
                eventIF.getContent(),
                eventIF.getSignature()));
  }

  public List<EventTagAddressTagPair> getEventTagAddressTagPairs(List<BaseTag> followSetsEvent) {
    return IntStream.range(0, followSetsEvent
            .stream()
            .filter(Predicate.not(IdentifierTag.class::isInstance))
            .toList().size() / 2)
        .map(i -> i * 2)
        .mapToObj(i -> new EventTagAddressTagPair((EventTag) followSetsEvent
            .stream()
            .filter(Predicate.not(IdentifierTag.class::isInstance))
            .toList().get(i), (AddressTag) followSetsEvent
            .stream()
            .filter(Predicate.not(IdentifierTag.class::isInstance))
            .toList().get(i + 1)))
        .toList();
  }

  @SneakyThrows
  private EventIF createFollowSetsEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull List<EventTagAddressTagPair> eventTagAddressTagPairs) {

    return new FollowSetsEvent(
        aImgIdentity,
        voteReceiverPubkey,
        eventTagAddressTagPairs,
//        TODO: replace 999999
        "99999").getGenericEventRecord();
  }

  @SneakyThrows
  private void deletePreviousReputationCalculationEvent(EventIF previousReputationEvent) {
    redisCacheServiceIF.deleteEvent(
        new DeletionEvent(
            aImgIdentity,
            List.of(new EventTag(previousReputationEvent.getId())), "aImg delete previous REPUTATION event"));
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
