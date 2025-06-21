package com.prosilion.afterimage.event.type;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.ReputationEvent;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.NostrException;
import com.prosilion.nostr.event.GenericEventKindIF;
import com.prosilion.nostr.event.GenericEventKindType;
import com.prosilion.nostr.event.GenericEventKindTypeIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.dto.EventDto;
import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventKindPlugin;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.RedisCache;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VoteEventKindTypePlugin extends AbstractNonPublishingEventKindPlugin {
  private final Identity aImgIdentity;
  private final EventEntityService eventEntityService;
  private final ReputationEventTypePlugin reputationEventTypePlugin;
  private final String afterimageRelayUrl;

  @Autowired
  public VoteEventKindTypePlugin(
      @NonNull RedisCache redisCache,
      @NonNull EventEntityService eventEntityService,
      @NonNull ReputationEventTypePlugin reputationEventTypePlugin,
      @NonNull Identity aImgIdentity,
      @NonNull String afterimageRelayUrl) {
    super(redisCache);
    this.eventEntityService = eventEntityService;
    this.reputationEventTypePlugin = reputationEventTypePlugin;
    this.aImgIdentity = aImgIdentity;
    this.afterimageRelayUrl = afterimageRelayUrl;
  }

  @SneakyThrows
  @Override
  public void processIncomingNonPublishingEventKind(@NonNull GenericEventKindIF voteEvent) {
    log.debug("processing incoming VOTE EVENT: [{}]", voteEvent);
//    saves VOTE event without triggering subscriber listener
    save(voteEvent);

    GenericEventKindType reputationEvent = calculateReputationEvent(voteEvent);
    save(reputationEvent);

    reputationEventTypePlugin.processIncomingPublishingEventKindType(reputationEvent);
  }

  private GenericEventKindType calculateReputationEvent(GenericEventKindIF event) throws URISyntaxException, NostrException, NoSuchAlgorithmException {
    List<GenericEventKindType> eventsByKindAndUpvoteOrDownvote = eventEntityService
        .getEventsByKind(Kind.BADGE_AWARD_EVENT).stream().map(GenericEventKindType::new).filter(t ->
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
        .reduce(BigDecimal::add).orElseThrow();

    return createReputationEvent(event.getPublicKey(), reputationCalculation, new URI(afterimageRelayUrl));
  }

  private GenericEventKindType createReputationEvent(@NonNull PublicKey badgeReceiverPubkey, @NonNull BigDecimal score, @NonNull URI uri) throws NostrException, NoSuchAlgorithmException {
    GenericEventKindType reputationEvent = new GenericEventKindType(new EventDto(
        new ReputationEvent(
            aImgIdentity,
            badgeReceiverPubkey,
            score,
            uri)).convertBaseEventToDto());
    return reputationEvent;
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;  // 2112 EVENT is an incoming vote from a person
  }
}
