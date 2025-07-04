package com.prosilion.afterimage.service.event.plugin;

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
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class ReputationEventKindTypePlugin implements EventKindTypePluginIF<KindTypeIF> {
  private final EventEntityService eventEntityService;
  private final Identity aImgIdentity;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;

  public ReputationEventKindTypePlugin(
      @NonNull EventEntityService eventEntityService,
      @NonNull Identity aImgIdentity,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    this.eventEntityService = eventEntityService;
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
    this.aImgIdentity = aImgIdentity;
  }

  @Override
  public Kind getKind() {
    log.debug("ReputationEventKindTypePlugin getKind returning Kind.BADGE_AWARD_EVENT");
    return Kind.BADGE_AWARD_EVENT;
  }

  @Override
  public KindTypeIF getKindType() {
    log.debug("ReputationEventKindTypePlugin getKindType returning Kind.REPUTATION");
    return AfterimageKindType.REPUTATION;
  }

  @SneakyThrows
  @Override
  public void processIncomingEvent(@NonNull GenericEventKindIF voteEvent) {
    eventEntityService.saveEventEntity(
        calculateReputationEvent(voteEvent));
  }

  private GenericEventKindTypeIF calculateReputationEvent(GenericEventKindIF event) throws URISyntaxException, NostrException, NoSuchAlgorithmException {
// TODO: refactor when testing complete    
    PublicKey badgeReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, event).stream()
        .map(PubKeyTag::getPublicKey).findFirst().orElseThrow();

    List<GenericEventKindIF> pubKeyesVoteHistory = eventEntityService
        .getEventsByKind(Kind.BADGE_AWARD_EVENT).stream()
        .filter(t -> t.getPublicKey().equals(badgeReceiverPubkey)).toList();

    List<GenericEventKindType> eventsByKindAndUpvoteOrDownvote = pubKeyesVoteHistory.stream()
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
