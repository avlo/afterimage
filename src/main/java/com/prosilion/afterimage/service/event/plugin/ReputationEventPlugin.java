package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.event.BadgeAwardReputationEvent;
import com.prosilion.afterimage.service.reputation.ReputationCalculationServiceIF;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindPlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import com.prosilion.superconductor.lib.redis.entity.EventNosqlEntityIF;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class ReputationEventPlugin extends PublishingEventKindPlugin {
  private final ReputationCalculationServiceIF reputationCalculationServiceIF;
  private final RedisCacheServiceIF redisCacheServiceIF;
  private final Identity aImgIdentity;

  public ReputationEventPlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindPluginIF eventKindTypePlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull ReputationCalculationServiceIF reputationCalculationServiceIF) {
    super(notifierService, eventKindTypePlugin);
    this.redisCacheServiceIF = redisCacheServiceIF;
    this.reputationCalculationServiceIF = reputationCalculationServiceIF;
    this.aImgIdentity = aImgIdentity;
  }

  public void processIncomingEvent(@NonNull EventIF eventIF) throws NostrException {

//   TOCO: validate cast / alt sol'n
    BadgeAwardReputationEvent incomingReputationEvent = (BadgeAwardReputationEvent) eventIF;

    PublicKey voteReceiverPubkey = incomingReputationEvent.getBadgeReceiverPubkey();
    BadgeDefinitionReputationEvent badgeDefinitionReputationEvent = incomingReputationEvent.getBadgeDefinitionReputationEvent();

//    ExternalIdentityTag externalIdentityTag = badgeDefinitionReputationEvent.getExternalIdentityTags().getFirst();
//    List<AddressTag> list = incomingReputationEvent.getBadgeReputationDefinitionEvent().getExternalIdentityTags().stream().map(externalIdentityTag ->
//            new AddressTag(
//                externalIdentityTag.getKind(), new PublicKey(externalIdentityTag.getIdentifierTag().getUuid()), externalIdentityTag.getIdentifierTag()))
//        .toList();

    BadgeAwardReputationEvent dbPreviousReputationEvent = getDbPreviousReputationEventRxR(voteReceiverPubkey, badgeDefinitionReputationEvent);

    deletePreviousReputationCalculationEvent(dbPreviousReputationEvent);

    super.processIncomingEvent(
        reputationCalculationServiceIF.calculateReputationEvent(
            voteReceiverPubkey,
            dbPreviousReputationEvent,
            incomingReputationEvent));
  }

  public BadgeAwardReputationEvent getDbPreviousReputationEventRxR(
      PublicKey badgeReceiverPubkey,
      BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) {
    List<EventNosqlEntityIF> eventsByKindAndPubKeyTagAndAddressTag = redisCacheServiceIF
        .getEventsByKindAndPubKeyTagAndAddressTag(
            Kind.BADGE_AWARD_EVENT,
            badgeReceiverPubkey,
            new AddressTag(
                Kind.BADGE_DEFINITION_EVENT,
                badgeDefinitionReputationEvent.getPublicKey(),
                badgeDefinitionReputationEvent.getIdentifierTag()));

    BadgeAwardReputationEvent badgeAwardReputationEvent = eventsByKindAndPubKeyTagAndAddressTag
        .stream()
        .max(Comparator.comparing(EventIF::getCreatedAt))
        .map(this::getGenericEventKind)
        .orElse(
            createGenericEventKindRxR(badgeReceiverPubkey, badgeDefinitionReputationEvent));
    return badgeAwardReputationEvent;
  }

  private BadgeAwardReputationEvent createGenericEventKindRxR(
      PublicKey badgeReceiverPubkey,
      BadgeDefinitionReputationEvent badgeDefinitionReputationEvent) {
    BadgeAwardReputationEvent badgeAwardReputationEvent = new BadgeAwardReputationEvent(
        aImgIdentity,
        badgeReceiverPubkey,
        badgeDefinitionReputationEvent,
        BigDecimal.ZERO);
    return badgeAwardReputationEvent;
  }

  private BadgeAwardReputationEvent getGenericEventKind(EventNosqlEntityIF eventIF) {
    Optional<BadgeAwardReputationEvent> badgeAwardReputationEvent = Optional.of(eventIF)
        .filter(BadgeAwardReputationEvent.class::isInstance)
        .map(BadgeAwardReputationEvent.class::cast);
    return badgeAwardReputationEvent.orElseThrow();
  }

  private void deletePreviousReputationCalculationEvent(EventIF previousReputationEvent) {
    redisCacheServiceIF.deleteEvent(
        new DeletionEvent(
            aImgIdentity,
            List.of(new EventTag(previousReputationEvent.getId())), "aImg delete previous REPUTATION event"));
  }

  @Override
  public Kind getKind() {
    log.debug("{} getKind returning {}}", getClass().getSimpleName(), super.getKind());
    return super.getKind();
  }
}
