package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.InvalidReputationCalculatorException;
import com.prosilion.afterimage.InvalidTagException;
import com.prosilion.afterimage.MissingIdentifierTagException;
import com.prosilion.afterimage.calculator.ReputationCalculatorIF;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKind;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindType;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindTypePlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class ReputationEventPlugin extends PublishingEventKindTypePlugin {
  private final Map<String, ReputationCalculatorIF> calculatorMap;
  private final RedisCacheServiceIF redisCacheServiceIF;
  private final Identity aImgIdentity;

  public ReputationEventPlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindTypePluginIF eventKindTypePlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull List<ReputationCalculatorIF> calculatorIFS) {
    super(notifierService, eventKindTypePlugin);
    this.redisCacheServiceIF = redisCacheServiceIF;
    this.calculatorMap = calculatorIFS.stream()
        .collect(
            Collectors.toMap(
                ReputationCalculatorIF::getFullyQualifiedCalculatorName,
                Function.identity(),
                (prev, next) -> next, HashMap::new));
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

    IdentifierTag identifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, incomingReputationEvent)
        .stream()
        .findFirst().orElseThrow(MissingIdentifierTagException::new);

    super.processIncomingEvent(
        Optional.ofNullable(
                calculatorMap.get(
                    identifierTag.getUuid()))
            .orElseThrow(() ->
                new InvalidReputationCalculatorException(identifierTag.getUuid(), calculatorMap.keySet().stream().toList()))
            .calculateUpdatedReputationEvent(
                voteReceiverPubkey,
                previousReputationEvent,
                incomingReputationEvent));
  }

  @SneakyThrows
  private void deletePreviousReputationCalculationEvent(GenericEventKindType previousReputationEvent) {
    redisCacheServiceIF.deleteEvent(
        new DeletionEvent(
            aImgIdentity,
            List.of(new EventTag(previousReputationEvent.getId())), "aImg delete previous REPUTATION event"));
  }

  public Optional<GenericEventKindType> getExistingReputationEvent(PublicKey badgeReceiverPubkey) {
    return redisCacheServiceIF
        .getEventsByKindAndPubKeyTag(Kind.BADGE_AWARD_EVENT, badgeReceiverPubkey)
        .stream()
        .filter(eventIF1 -> eventIF1.getTags()
            .stream()
            .filter(AddressTag.class::isInstance)
            .map(AddressTag.class::cast)
            .anyMatch(addressTag ->
                Optional.ofNullable(
                        addressTag.getIdentifierTag()).orElseThrow(() ->
                        new InvalidTagException("NULL", List.of(getKindType().getName())))
                    .getUuid().equals(getKindType().getName()))).toList().stream()
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
