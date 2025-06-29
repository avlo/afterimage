package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.ReputationEvent;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.GenericEventKindIF;
import com.prosilion.nostr.event.GenericEventKindType;
import com.prosilion.nostr.event.GenericEventKindTypeIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.dto.GenericEventKindTypeDto;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.NonPublishingEventKindTypePlugin;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public abstract class VoteEventKindTypePlugin extends NonPublishingEventKindTypePlugin {
  private final Identity aImgIdentity;
  private final EventEntityService eventEntityService;
  private final ReputationEventKindTypePlugin reputationEventKindTypePlugin;
  private final String afterimageRelayUrl;

  public VoteEventKindTypePlugin(
      @NonNull EventEntityService eventEntityService,
      @NonNull ReputationEventKindTypePlugin reputationEventKindTypePlugin,
      @NonNull Identity aImgIdentity,
      @NonNull String afterimageRelayUrl) {
    super(reputationEventKindTypePlugin);
    this.eventEntityService = eventEntityService;
    this.reputationEventKindTypePlugin = reputationEventKindTypePlugin;
    this.aImgIdentity = aImgIdentity;
    this.afterimageRelayUrl = afterimageRelayUrl;
  }

  @SneakyThrows
  @Override
  public void processIncomingEvent(@NonNull GenericEventKindTypeIF voteEvent) {
    log.debug("processing incoming VOTE EVENT: [{}]", voteEvent);
//    saves VOTE event without triggering subscriber listener
    super.processIncomingEvent(voteEvent);
    GenericEventKindTypeIF reputationEvent = calculateReputationEvent(voteEvent);
    reputationEventKindTypePlugin.processIncomingEvent(reputationEvent);
  }

  private GenericEventKindTypeIF calculateReputationEvent(GenericEventKindIF event) throws URISyntaxException, NostrException, NoSuchAlgorithmException {

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
                AfterimageKindType.REPUTATION)).toList();

    BigDecimal reputationCalculation = eventsByKindAndUpvoteOrDownvote.stream()
        .map(GenericEventKindTypeIF::getContent)
        .map(BigDecimal::new)
        .reduce(BigDecimal::add).orElse(BigDecimal.ZERO).add(new BigDecimal(event.getContent()));

    return createReputationEvent(badgeReceiverPubkey, reputationCalculation, new URI(afterimageRelayUrl));
  }

  private GenericEventKindTypeIF createReputationEvent(@NonNull PublicKey badgeReceiverPubkey, @NonNull BigDecimal score, @NonNull URI uri) throws NostrException, NoSuchAlgorithmException {
    GenericEventKindTypeIF reputationEvent =
        new GenericEventKindTypeDto(
            new ReputationEvent(
                aImgIdentity,
                badgeReceiverPubkey,
                score,
                uri),
            AfterimageKindType.REPUTATION).convertBaseEventToGenericEventKindTypeIF();
    return reputationEvent;
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;  // 2112 EVENT is an incoming vote from a person
  }

  abstract public KindTypeIF getKindType();
}
