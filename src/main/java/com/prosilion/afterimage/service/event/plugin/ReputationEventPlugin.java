package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.service.AfterimageReputationCalculator;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
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
import com.prosilion.superconductor.lib.redis.document.EventDocumentIF;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class ReputationEventPlugin extends PublishingEventKindTypePlugin {
  private final AfterimageReputationCalculator calculator;
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
    this.calculator = afterimageReputationCalculator;
    this.aImgIdentity = aImgIdentity;
  }

  @SneakyThrows
  public void processIncomingEvent(@NonNull EventIF incomingReputationEvent) {
    PublicKey voteReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, incomingReputationEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

    Optional<GenericEventKindType> previousReputationEvent = getExistingReputationEvent(voteReceiverPubkey);
    previousReputationEvent.ifPresent(this::deletePreviousReputationCalculationEvent);

    GenericEventKindTypeIF updatedReputationEvent =
        calculator.calculateUpdatedReputationEvent(
            voteReceiverPubkey,
            previousReputationEvent,
            incomingReputationEvent);

    super.processIncomingEvent(updatedReputationEvent);
  }

  @SneakyThrows
  private void deletePreviousReputationCalculationEvent(GenericEventKindType previousReputationEvent) {
    redisCacheServiceIF.deleteEvent(
        new DeletionEvent(
            aImgIdentity,
            List.of(new EventTag(previousReputationEvent.getId())), "aImg delete previous REPUTATION event"));
  }

  public Optional<GenericEventKindType> getExistingReputationEvent(PublicKey badgeReceiverPubkey) {
    List<EventDocumentIF> eventsByKindAndPubKeyTag = redisCacheServiceIF
        .getEventsByKindAndPubKeyTag(Kind.BADGE_AWARD_EVENT, badgeReceiverPubkey);

    List<EventDocumentIF> eventDocumentIFStream = eventsByKindAndPubKeyTag
        .stream()
        .filter(eventIF -> eventIF.getTags()
            .stream()
            .filter(AddressTag.class::isInstance)
            .map(AddressTag.class::cast)
            .anyMatch(addressTag ->
                Optional.ofNullable(
                        addressTag.getIdentifierTag()).orElseThrow(() ->
                        new InvalidTagException("NULL", List.of(getKindType().getName())))
                    .getUuid().equals(getKindType().getName()))).toList();
    Optional<EventDocumentIF> max = eventDocumentIFStream.stream()
        .max(Comparator.comparing(EventIF::getCreatedAt));

    return max
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
