package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.AddressableEvent;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.NonPublishingEventKindPlugin;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.DurationFactory;
import org.springframework.lang.NonNull;

@Slf4j
// our SportsCar extends CarDecorator
public abstract class AbstractVoteEventPlugin extends NonPublishingEventKindPlugin {
  private final CacheServiceIF cacheServiceIF;
  private final CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService;
  private final CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService;
  private final CacheFollowSetsEventService cacheFollowSetsEventService;
  private final AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin;
  private final Identity aImgIdentity;
  private final Relay relay;

  public AbstractVoteEventPlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
      @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
      @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
      @NonNull AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin,
      @NonNull EventPlugin eventPlugin,
      @NonNull Identity aImgIdentity) {
    super(eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.cacheBadgeDefinitionGenericEventService = cacheBadgeDefinitionGenericEventService;
    this.cacheBadgeDefinitionReputationEventService = cacheBadgeDefinitionReputationEventService;
    this.cacheFollowSetsEventService = cacheFollowSetsEventService;
    this.afterimageFollowSetsEventPlugin = afterimageFollowSetsEventPlugin;
    this.relay = new Relay(afterimageRelayUrl);
  }

  @Override
  @SneakyThrows
  public GenericEventRecord processIncomingEvent(@NonNull EventIF voteEvent) {
    log.debug("processing incoming Kind[{}]:{}\n{}",
        voteEvent.getKind().getValue(),
        voteEvent.getKind().getName().toUpperCase(),
        voteEvent.createPrettyPrintJson());

    PublicKey awardRecipientPublicKey = Filterable.getTypeSpecificTags(PubKeyTag.class, voteEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

    AddressTag addressTag = Filterable.getTypeSpecificTagsStream(AddressTag.class, voteEvent).findFirst().orElseThrow();

    BadgeDefinitionGenericEvent badgeDefinitionAwardEvent = cacheBadgeDefinitionGenericEventService.getAddressTagEvent(addressTag, DurationFactory.of(10L, TimeUnit.SECONDS)).orElseThrow();

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> reconstructedVoteEvent =
        new BadgeAwardGenericEvent<>(voteEvent.asGenericEventRecord(), aTag -> badgeDefinitionAwardEvent);

    log.debug("reconstructedVoteEvent:\n{}", reconstructedVoteEvent.createPrettyPrintJson());

    List<BadgeDefinitionReputationEvent> badgeDefinitionReputationEventCandidates = cacheBadgeDefinitionReputationEventService.getExistingReputationDefinitionEvents();

    List<AddressTag> formulasAsAddressTags = badgeDefinitionReputationEventCandidates.stream()
        .flatMap(badgeDefinitionReputationEvent ->
            badgeDefinitionReputationEvent.getFormulaEvents()
                .stream().map(AddressableEvent::asAddressTag)).toList();

    List<BadgeDefinitionReputationEvent> matchingBadgeDefinitionReputationEvents =
        badgeDefinitionReputationEventCandidates.stream()
            .filter(badgeDefinitionReputationEvent ->
                badgeDefinitionReputationEvent
                    .getFormulaEvents().stream()
                    .map(FormulaEvent::asAddressTag).toList().stream().anyMatch(formulasAsAddressTags::contains)).toList();

    List<FollowSetsEvent> awardRecipientExistingFollowSets =
        cacheServiceIF.getEventsByKindAndPubKeyTag(Kind.FOLLOW_SETS, awardRecipientPublicKey).stream()
            .map(cacheFollowSetsEventService::materialize)
            .filter(followSetsEvent ->
                matchingBadgeDefinitionReputationEvents.stream()
                    .map(BadgeDefinitionReputationEvent::getIdentifierTag)
                    .anyMatch(followSetsEvent.getIdentifierTag()::equals)).toList();

    List<FollowSetsEvent> awardRecipientDifferentiatedFollowSetsEvents = awardRecipientExistingFollowSets.stream().map(followSetsEvent ->
        createFollowSetsEvent(
            awardRecipientPublicKey,
            followSetsEvent.getIdentifierTag(),
            Stream.concat(
                    followSetsEvent.getBadgeAwardGenericEvents().stream(),
                    Stream.of(reconstructedVoteEvent))
                .toList())).toList();

    List<FollowSetsEvent> listToSend = !awardRecipientDifferentiatedFollowSetsEvents.isEmpty() ?
        awardRecipientDifferentiatedFollowSetsEvents
        :
        matchingBadgeDefinitionReputationEvents.stream().map(badgeDefinitionReputationEvent ->
                createFollowSetsEvent(
                    awardRecipientPublicKey,
                    badgeDefinitionReputationEvent.getIdentifierTag(),
                    List.of(reconstructedVoteEvent)))
            .toList();

    listToSend.stream().map(afterimageFollowSetsEventPlugin::processIncomingEvent).toList();
    return reconstructedVoteEvent.asGenericEventRecord();
  }

  @SneakyThrows
  private FollowSetsEvent createFollowSetsEvent(
      @NonNull PublicKey reputationRecipientPublicKey,
      @NonNull IdentifierTag identifierTag,
      @NonNull List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> badgeAwardGenericVoteEvent) {
    return new FollowSetsEvent(
        aImgIdentity,
        reputationRecipientPublicKey,
        identifierTag,
        relay,
        badgeAwardGenericVoteEvent);
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }
}
