package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.InvalidTagException;
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
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.CacheIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindTypePlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import com.prosilion.superconductor.lib.jpa.dto.GenericEventKindTypeDto;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class ReputationPublishingEventKindTypePlugin extends PublishingEventKindTypePlugin {
  private final CacheIF cacheIF;
  private final Identity aImgIdentity;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;

  public ReputationPublishingEventKindTypePlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindTypePluginIF<KindTypeIF> eventKindTypePlugin,
      @NonNull CacheIF cacheIF,
      @NonNull Identity aImgIdentity,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    super(notifierService, eventKindTypePlugin);
    this.cacheIF = cacheIF;
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.aImgIdentity = aImgIdentity;
  }

  @SneakyThrows
  public void processIncomingEvent(@NonNull GenericEventKindIF voteEvent) {
    super.processIncomingEvent(
        calculateReputationEvent(voteEvent));
  }

  private GenericEventKindTypeIF calculateReputationEvent(GenericEventKindIF event) throws NostrException, NoSuchAlgorithmException {
    PublicKey badgeReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, event).stream()
        .map(PubKeyTag::getPublicKey).findFirst().orElseThrow();

    return createReputationEvent(
        badgeReceiverPubkey,
        cacheIF
            .getEventsByKind(Kind.BADGE_AWARD_EVENT).stream().map(e ->
                cacheIF.getByEventIdString(e.getId()))
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
                        .getUuid().equals(getKindType().getName())))
            .map(genericEventKind ->
                new GenericEventKindType(
                    genericEventKind.orElseThrow().getId(),
                    genericEventKind.orElseThrow().getPublicKey(),
                    genericEventKind.orElseThrow().getCreatedAt(),
                    genericEventKind.orElseThrow().getKind(),
                    genericEventKind.orElseThrow().getTags(),
                    genericEventKind.orElseThrow().getContent(),
                    genericEventKind.orElseThrow().getSignature(),
                    getKindType())).toList().stream()
            .map(GenericEventKindTypeIF::getContent)
            .map(BigDecimal::new).toList().stream()
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO));
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
