package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.AddressableEvent;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.cache.CacheBadgeAwardGenericEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheBadgeDefinitionGenericEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheBadgeDefinitionReputationEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheFormulaEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.NonPublishingEventKindPlugin;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
// our SportsCar extends CarDecorator
public abstract class AbstractVoteEventPlugin extends NonPublishingEventKindPlugin {
  private final CacheServiceIF cacheServiceIF;
  private final CacheBadgeDefinitionReputationEventServiceIF cacheBadgeDefinitionReputationEventServiceIF;
  private final CacheFormulaEventServiceIF cacheFormulaEventServiceIF;
  private final CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF;
  private final CacheBadgeAwardGenericEventServiceIF<BadgeDefinitionGenericEvent, BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> cacheBadgeAwardGenericEventServiceIF;
  private final CacheBadgeDefinitionGenericEventServiceIF cacheBadgeDefinitionGenericEventServiceIF;
  private final AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin;
  private final Identity aImgIdentity;
  private final Relay relay;

  public AbstractVoteEventPlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull CacheBadgeDefinitionGenericEventServiceIF cacheBadgeDefinitionGenericEventServiceIF,
      @NonNull CacheFormulaEventServiceIF cacheFormulaEventServiceIF,
      @NonNull CacheBadgeDefinitionReputationEventServiceIF cacheBadgeDefinitionReputationEventServiceIF,
      @NonNull CacheBadgeAwardGenericEventServiceIF<BadgeDefinitionGenericEvent, BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> cacheBadgeAwardGenericEventServiceIF,
      @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
      @NonNull AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin,
      @NonNull EventKindPluginIF eventKindPluginIF,
      @NonNull Identity aImgIdentity) {
    super(eventKindPluginIF);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.cacheBadgeDefinitionGenericEventServiceIF = cacheBadgeDefinitionGenericEventServiceIF;
    this.cacheFormulaEventServiceIF = cacheFormulaEventServiceIF;
    this.cacheBadgeAwardGenericEventServiceIF = cacheBadgeAwardGenericEventServiceIF;
    this.cacheBadgeDefinitionReputationEventServiceIF = cacheBadgeDefinitionReputationEventServiceIF;
    this.cacheFollowSetsEventServiceIF = cacheFollowSetsEventServiceIF;
    this.afterimageFollowSetsEventPlugin = afterimageFollowSetsEventPlugin;
    this.relay = new Relay(afterimageRelayUrl);
  }

  @Override
  public <T extends BaseEvent> void processIncomingEvent(@NonNull T voteEvent) {
    log.debug("\n\n\n#########  voteEvent class: [{}]\n\n", voteEvent.getClass().getSimpleName());
    log.debug("processing incoming Kind[{}]:{}\n{}",
        voteEvent.getKind().getValue(),
        voteEvent.getKind().getName().toUpperCase(),
        voteEvent.createPrettyPrintJson());

    PublicKey awardRecipientPublicKey = Filterable.getTypeSpecificTags(PubKeyTag.class, voteEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

//    AddressTag addressTag = Filterable.getTypeSpecificTagsStream(AddressTag.class, voteEvent).findFirst().orElseThrow();
//    BadgeDefinitionGenericEvent badgeDefinitionAwardEvent = cacheBadgeDefinitionGenericEventServiceIF.getAddressTagEvent(addressTag).orElseThrow();

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> reconstructedVoteEvent =
//        new BadgeAwardGenericEvent<>(voteEvent.asGenericEventRecord(), aTag -> badgeDefinitionAwardEvent);
        cacheBadgeAwardGenericEventServiceIF.materialize(voteEvent);

    log.debug("reconstructedVoteEvent:\n{}", reconstructedVoteEvent.createPrettyPrintJson());

    List<BadgeDefinitionReputationEvent> badgeDefinitionReputationEventCandidates = cacheBadgeDefinitionReputationEventServiceIF.getExistingReputationDefinitionEvents();

//    List<AddressTag> formulasTarget = cacheFormulaEventServiceIF.getEvent(
//        badgeDefinitionAwardEvent.getId(),
//        badgeDefinitionAwardEvent.getRelayTagRelay().getUrl()).stream().map(FormulaEvent::asAddressTag).toList();

    List<AddressTag> formulasAsAddressTags = badgeDefinitionReputationEventCandidates.stream()
        .flatMap(badgeDefinitionReputationEvent ->
            badgeDefinitionReputationEvent.getFormulaEvents()
                .stream().map(AddressableEvent::asAddressTag)).toList();

    List<BadgeDefinitionReputationEvent> matchingBadgeDefinitionReputationEvents =
        badgeDefinitionReputationEventCandidates.stream()
            .filter(badgeDefinitionReputationEvent ->
            {
              List<AddressTag> formulaEvents = badgeDefinitionReputationEvent
                  .getFormulaEvents().stream()
                  .map(FormulaEvent::asAddressTag).toList();
              boolean match = formulaEvents.stream().anyMatch(formulasAsAddressTags::contains);
              return match;
            }).toList();

//    cacheServiceIF.getEventsByKindAndPubKeyTag(getKind(), badgeAwardRecipientPublicKey);

    List<FollowSetsEvent> awardRecipientExistingFollowSets =
        cacheServiceIF.getEventsByKindAndPubKeyTag(Kind.FOLLOW_SETS, awardRecipientPublicKey).stream()
            .map(cacheFollowSetsEventServiceIF::materialize)
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

    List<FollowSetsEvent> listToSend = !awardRecipientDifferentiatedFollowSetsEvents.isEmpty() ? awardRecipientDifferentiatedFollowSetsEvents
        :
        matchingBadgeDefinitionReputationEvents.stream().map(badgeDefinitionReputationEvent ->
                createFollowSetsEvent(
                    awardRecipientPublicKey,
                    badgeDefinitionReputationEvent.getIdentifierTag(),
                    List.of(reconstructedVoteEvent)))
            .toList();

    listToSend.stream().forEach(afterimageFollowSetsEventPlugin::processIncomingEvent);
  }

  @SneakyThrows
  private FollowSetsEvent createFollowSetsEvent(
      @NonNull PublicKey reputationRecipientPublicKey,
      @NonNull IdentifierTag identifierTag,
      @NonNull List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> badgeAwardGenericVoteEvent) {
    FollowSetsEvent followSetsEvent = new FollowSetsEvent(
        aImgIdentity,
        reputationRecipientPublicKey,
        identifierTag,
        relay,
        badgeAwardGenericVoteEvent);
    return followSetsEvent;
  }

  @Override
  public BaseEvent materialize(EventIF eventIF) {
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> materialize = cacheBadgeAwardGenericEventServiceIF.materialize(eventIF);
    return materialize;
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }
}
