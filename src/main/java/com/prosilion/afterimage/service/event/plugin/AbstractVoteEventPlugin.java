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
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
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
import java.util.Optional;
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
    PublicKey awardRecipientPublicKey = Filterable.getTypeSpecificTags(PubKeyTag.class, voteEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

    AddressTag addressTag = Filterable.getTypeSpecificTagsStream(AddressTag.class, voteEvent).findFirst().orElseThrow();
    BadgeDefinitionGenericEvent badgeDefinitionAwardEvent = cacheBadgeDefinitionGenericEventServiceIF.getAddressTagEvent(addressTag).orElseThrow();

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> reconstructedVoteEvent = new BadgeAwardGenericEvent<>(voteEvent.asGenericEventRecord(), aTag -> badgeDefinitionAwardEvent);

    List<BadgeDefinitionReputationEvent> badgeDefinitionReputationEventCandidates = cacheBadgeDefinitionReputationEventServiceIF.getExistingReputationDefinitionEvents();

//    List<AddressTag> formulasTarget = cacheFormulaEventServiceIF.getEvent(
//        badgeDefinitionAwardEvent.getId(),
//        badgeDefinitionAwardEvent.getRelayTagRelay().getUrl()).stream().map(FormulaEvent::asAddressTag).toList();

    List<AddressTag> formulasTarget = badgeDefinitionReputationEventCandidates.stream()
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
              boolean match = formulaEvents.stream().anyMatch(formulasTarget::contains);
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
            followSetsEvent.getTypeSpecificTags(PubKeyTag.class).stream().map(PubKeyTag::getPublicKey).findFirst().orElseThrow(),
            followSetsEvent.getIdentifierTag(),
            Stream.concat(
                    followSetsEvent.getBadgeAwardGenericEvents().stream(),
                    Stream.of(reconstructedVoteEvent))
                .toList())).toList();

    List<FollowSetsEvent> listToSend = !awardRecipientDifferentiatedFollowSetsEvents.isEmpty() ? awardRecipientDifferentiatedFollowSetsEvents :
        cacheServiceIF.getByKind(
                Kind.BADGE_DEFINITION_EVENT).stream().filter(ger ->
                !Filterable.getTypeSpecificTags(ExternalIdentityTag.class, ger).isEmpty())
            .map(genericEventRecord ->
                cacheBadgeDefinitionReputationEventServiceIF.getEvent(genericEventRecord.getId(),
                        Filterable.getTypeSpecificTagsStream(RelayTag.class, genericEventRecord).findFirst().map(RelayTag::getRelay).map(Relay::getUrl).orElseThrow())
                    .map(AddressableEvent::getIdentifierTag).map(identifierTag ->
                        createFollowSetsEvent(
                            reconstructedVoteEvent.getTypeSpecificTags(PubKeyTag.class).stream().map(PubKeyTag::getPublicKey).findFirst().orElseThrow(),
                            identifierTag,
                            List.of(reconstructedVoteEvent))))
            .flatMap(Optional::stream)
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
