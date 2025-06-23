package com.prosilion.afterimage.event.type;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.ReputationEvent;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.enums.NostrException;
import com.prosilion.nostr.event.GenericEventKindIF;
import com.prosilion.nostr.event.GenericEventKindType;
import com.prosilion.nostr.event.GenericEventKindTypeIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.dto.GenericEventKindTypeDto;
import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventKindPlugin;
import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventKindTypePlugin;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public abstract class VoteEventKindTypePlugin extends AbstractNonPublishingEventKindTypePlugin {
  private final Identity aImgIdentity;
  private final EventEntityService eventEntityService;
  private final ReputationEventKindTypePlugin reputationEventKindTypePlugin;
  private final String afterimageRelayUrl;

  public VoteEventKindTypePlugin(
      @NonNull AbstractNonPublishingEventKindPlugin abstractNonPublishingEventKindPlugin,
      @NonNull EventEntityService eventEntityService,
      @NonNull ReputationEventKindTypePlugin reputationEventKindTypePlugin,
      @NonNull Identity aImgIdentity
//      , @NonNull String afterimageRelayUrl
  ) {
    super(abstractNonPublishingEventKindPlugin);
    this.eventEntityService = eventEntityService;
    this.reputationEventKindTypePlugin = reputationEventKindTypePlugin;
    this.aImgIdentity = aImgIdentity;
//    this.afterimageRelayUrl = afterimageRelayUrl;
    this.afterimageRelayUrl = "ws://localhost:5556";
  }

  @SneakyThrows
  @Override
  public void processIncomingNonPublishingEventKindType(@NonNull GenericEventKindTypeIF voteEvent) {
    log.debug("processing incoming VOTE EVENT: [{}]", voteEvent);
//    saves VOTE event without triggering subscriber listener
    save(voteEvent);

    GenericEventKindTypeIF reputationEvent = calculateReputationEvent(voteEvent);
    save(reputationEvent);

    reputationEventKindTypePlugin.processIncomingPublishingEventKindType(reputationEvent);
  }

  private GenericEventKindTypeIF calculateReputationEvent(GenericEventKindIF event) throws URISyntaxException, NostrException, NoSuchAlgorithmException {
    List<GenericEventKindType> eventsByKindAndUpvoteOrDownvote = eventEntityService
        .getEventsByKind(Kind.BADGE_AWARD_EVENT).stream().map(genericEventKindIF ->
            new GenericEventKindType(
                event.getId(),
                event.getPublicKey(),
                event.getCreatedAt(),
                event.getKind(),
                event.getTags(),
                event.getContent(),
                event.getSignature(),
                List.of(AfterimageKindType.values()))).filter(t ->
            Filterable.getTypeSpecificTags(AddressTag.class, t).stream()
                .filter(addressTag ->
                    !addressTag.getIdentifierTag().getUuid().equals(
                        AfterimageKindType.REPUTATION.getName())).isParallel()).toList();

    List<GenericEventKindType> pubKeyesVoteHistory = eventsByKindAndUpvoteOrDownvote.stream().collect(
        Collectors.teeing(
            Collectors.filtering(t -> t.getPublicKey().equals(
                event.getPublicKey()), Collectors.toList()),
            Collectors.filtering(t -> t.getPublicKey().equals(
                    Filterable.getTypeSpecificTags(AddressTag.class, t).stream()
                        .map(AddressTag::getPublicKey)
                        .filter(publicKey -> publicKey.equals(t.getPublicKey())).toList()),
                Collectors.toList()),
            List::of)).stream().flatMap(List::stream).toList();

    BigDecimal reputationCalculation = pubKeyesVoteHistory.stream()
        .map(GenericEventKindTypeIF::getContent)
        .map(BigDecimal::new)
        .reduce(BigDecimal::add).orElse(BigDecimal.ZERO).add(new BigDecimal(event.getContent()));

    return createReputationEvent(event.getPublicKey(), reputationCalculation, new URI(afterimageRelayUrl));
  }

  private GenericEventKindTypeIF createReputationEvent(@NonNull PublicKey badgeReceiverPubkey, @NonNull BigDecimal score, @NonNull URI uri) throws NostrException, NoSuchAlgorithmException {
    GenericEventKindTypeIF reputationEvent =
        new GenericEventKindTypeDto(
            new ReputationEvent(
                aImgIdentity,
                badgeReceiverPubkey,
                score,
                uri),
            List.of(AfterimageKindType.values())).convertBaseEventToGenericEventKindTypeIF();
    return reputationEvent;
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;  // 2112 EVENT is an incoming vote from a person
  }

  abstract public KindTypeIF getKindType();
}
