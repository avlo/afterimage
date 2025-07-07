package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.config.PublishingEventKindTypePlugin;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.GenericEventKindIF;
import com.prosilion.nostr.event.GenericEventKindType;
import com.prosilion.nostr.event.GenericEventKindTypeIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.dto.GenericEventKindTypeDto;
import com.prosilion.superconductor.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.request.NotifierService;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class ReputationEventKindTypePlugin extends PublishingEventKindTypePlugin {
  private final EventEntityService eventEntityService;
  private final Identity aImgIdentity;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;

  public ReputationEventKindTypePlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindTypePluginIF<KindTypeIF> eventKindTypePlugin,
      @NonNull EventEntityService eventEntityService,
      @NonNull Identity aImgIdentity,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    super(notifierService, eventKindTypePlugin);
    this.eventEntityService = eventEntityService;
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.aImgIdentity = aImgIdentity;
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

  @SneakyThrows
  public void processIncomingEvent(@NonNull GenericEventKindIF voteEvent) {
    GenericEventKindTypeIF event = calculateReputationEvent(voteEvent);
    super.processIncomingEvent(event);
  }

  private GenericEventKindTypeIF calculateReputationEvent(GenericEventKindIF event) throws NostrException, NoSuchAlgorithmException {
// TODO: refactor when testing complete    
    PublicKey badgeReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, event).stream()
        .map(PubKeyTag::getPublicKey).findFirst().orElseThrow();

    List<GenericEventKindIF> fullyPopulatedEventKind8s = eventEntityService
        .getEventsByKind(Kind.BADGE_AWARD_EVENT).stream().map(e ->
            eventEntityService.findByEventIdString(e.getId()))
        .map(a -> eventEntityService.getEventById(a.orElseThrow().getId()))
        .toList();

    List<GenericEventKindIF> publicKeyMatchList = fullyPopulatedEventKind8s.stream()
        .filter(genericEventKindIF -> genericEventKindIF.getTags()
            .stream()
            .filter(PubKeyTag.class::isInstance)
            .map(PubKeyTag.class::cast)
            .anyMatch(pubKeyTag -> pubKeyTag.getPublicKey().equals(badgeReceiverPubkey)))
        .toList();

    List<GenericEventKindType> eventsByKindAndUpvoteOrDownvote = publicKeyMatchList.stream()
        .map(genericEventKindIF ->
            new GenericEventKindType(
                genericEventKindIF.getId(),
                genericEventKindIF.getPublicKey(),
                genericEventKindIF.getCreatedAt(),
                genericEventKindIF.getKind(),
                genericEventKindIF.getTags(),
                genericEventKindIF.getContent(),
                genericEventKindIF.getSignature(),
                getKindType())).toList();

    BigDecimal reputationCalculation = eventsByKindAndUpvoteOrDownvote.stream()
        .map(GenericEventKindTypeIF::getContent)
        .map(BigDecimal::new)
        .reduce(BigDecimal::add).orElse(BigDecimal.ZERO).add(new BigDecimal(event.getContent()));

    GenericEventKindTypeIF reputationEvent = createReputationEvent(badgeReceiverPubkey, reputationCalculation);
    return reputationEvent;
  }

  private GenericEventKindTypeIF createReputationEvent(@NonNull PublicKey badgeReceiverPubkey, @NonNull BigDecimal score) throws NostrException, NoSuchAlgorithmException {
    return new GenericEventKindTypeDto(
        new BadgeAwardReputationEvent(
            aImgIdentity,
            badgeReceiverPubkey,
            reputationBadgeDefinitionEvent,
            score),
        AfterimageKindType.REPUTATION).convertBaseEventToGenericEventKindTypeIF();
  }
}
