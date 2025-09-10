package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.service.AfterimageReputationCalculator;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FollowSetsEvent.EventTagAddressTagPair;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKind;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindType;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindTypePlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class ReputationEventPlugin extends PublishingEventKindTypePlugin {
  private final AfterimageReputationCalculator reputationCalculator;
  private final RedisCacheServiceIF redisCacheServiceIF;
  private final Identity aImgIdentity;

  public ReputationEventPlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindTypePluginIF eventKindTypePlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull AfterimageReputationCalculator afterimageReputationCalculator) {
    super(notifierService, eventKindTypePlugin);
    this.redisCacheServiceIF = redisCacheServiceIF;
    this.reputationCalculator = afterimageReputationCalculator;
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

    GenericEventKindTypeIF updatedReputationEvent =
        reputationCalculator.calculateUpdatedReputationEvent(
            voteReceiverPubkey,
            previousReputationEvent,
            incomingFollowSetsEvent);

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

  @SneakyThrows
  private void deletePreviousReputationCalculationEvent(GenericEventKindType previousReputationEvent) {
    redisCacheServiceIF.deleteEvent(
        new DeletionEvent(
            aImgIdentity,
            List.of(new EventTag(previousReputationEvent.getId())), "aImg delete previous REPUTATION event"));
  }

  private Optional<GenericEventKindType> getPreviousReputationEvent(PublicKey badgeReceiverPubkey) {
    return redisCacheServiceIF
        .getEventsByKindAndPubKeyTag(Kind.BADGE_AWARD_EVENT, badgeReceiverPubkey)
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
//                    aImgIdentity.getPublicKey(),
                    eventIF.getPublicKey(),
                    eventIF.getCreatedAt(),
                    eventIF.getKind(),
                    eventIF.getTags(),
                    eventIF.getContent(),
                    eventIF.getSignature()),
                getKindType()));
  }

//  public Optional<GenericEventKindType> getAllPubkeyReputationEvents(@NonNull PublicKey badgeReceiverPubkey) {
//    return getPreviousReputationEvent(badgeReceiverPubkey);
//  }

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

  @SneakyThrows
  private EventIF createFollowSetsEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull List<EventTagAddressTagPair> eventTagAddressTagPairs) {
    return new FollowSetsEvent(
        aImgIdentity,
        voteReceiverPubkey,
        eventTagAddressTagPairs,
        reputationCalculator.getClass().getName()).getGenericEventRecord();
  }

  private List<EventTagAddressTagPair> getEventTagAddressTagPairs(List<BaseTag> followSetsEvent) {
    List<EventTag> eventTags = followSetsEvent
        .stream()
        .filter(EventTag.class::isInstance)
        .map(EventTag.class::cast)
        .toList();

    List<AddressTag> addressTags = followSetsEvent
        .stream()
        .filter(AddressTag.class::isInstance)
        .map(AddressTag.class::cast)
        .toList();

    assert eventTags.size() == addressTags.size();

    List<EventTagAddressTagPair> pairs = IntStream.range(0, eventTags.size())
        .mapToObj(i -> new EventTagAddressTagPair(
            eventTags.get(i),
            addressTags.get(i)))
        .toList();
    return pairs;
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
