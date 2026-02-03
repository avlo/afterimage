package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.AddressableEvent;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
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
import com.prosilion.superconductor.base.service.CacheBadgeAwardGenericEventServiceIF;
import com.prosilion.superconductor.base.service.CacheBadgeDefinitionGenericEventServiceIF;
import com.prosilion.superconductor.base.service.CacheBadgeDefinitionReputationEventServiceIF;
import com.prosilion.superconductor.base.service.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.service.CacheFormulaEventServiceIF;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.type.NonPublishingEventKindPlugin;
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
  private final CacheBadgeAwardGenericEventServiceIF cacheBadgeAwardGenericEventServiceIF;
  private final CacheBadgeDefinitionReputationEventServiceIF cacheBadgeDefinitionReputationEventServiceIF;
  private final CacheFormulaEventServiceIF cacheFormulaEventServiceIF;
  private final CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF;
  private final CacheBadgeDefinitionGenericEventServiceIF cacheBadgeDefinitionGenericEventServiceIF;
  private final AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin;
  private final Identity aImgIdentity;
  private final Relay relay;

  public AbstractVoteEventPlugin(
      @NonNull String afterimageRelayUrl,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull CacheBadgeAwardGenericEventServiceIF cacheBadgeAwardGenericEventServiceIF,
      @NonNull CacheBadgeDefinitionGenericEventServiceIF cacheBadgeDefinitionGenericEventServiceIF,
      @NonNull CacheFormulaEventServiceIF cacheFormulaEventServiceIF,
      @NonNull CacheBadgeDefinitionReputationEventServiceIF cacheBadgeDefinitionReputationEventServiceIF,
      @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
      @NonNull AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin,
      @NonNull EventKindPluginIF eventKindPluginIF,
      @NonNull Identity aImgIdentity) {
    super(eventKindPluginIF);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.cacheBadgeAwardGenericEventServiceIF = cacheBadgeAwardGenericEventServiceIF;
    this.cacheBadgeDefinitionGenericEventServiceIF = cacheBadgeDefinitionGenericEventServiceIF;
    this.cacheFormulaEventServiceIF = cacheFormulaEventServiceIF;
    this.cacheBadgeDefinitionReputationEventServiceIF = cacheBadgeDefinitionReputationEventServiceIF;
    this.cacheFollowSetsEventServiceIF = cacheFollowSetsEventServiceIF;
    this.afterimageFollowSetsEventPlugin = afterimageFollowSetsEventPlugin;
    this.relay = new Relay(afterimageRelayUrl);
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF voteEvent) {
    PublicKey awardRecipientPublicKey = Filterable.getTypeSpecificTags(PubKeyTag.class, voteEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

    AddressTag addressTag = Filterable.getTypeSpecificTagsStream(AddressTag.class, voteEvent).findFirst().orElseThrow();
    BadgeDefinitionGenericEvent badgeDefinitionAwardEvent = cacheBadgeDefinitionGenericEventServiceIF.getEvent(addressTag).orElseThrow();

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> reconstructedVoteEvent = new BadgeAwardGenericEvent<>(voteEvent.asGenericEventRecord(), aTag -> badgeDefinitionAwardEvent);

    List<BadgeDefinitionReputationEvent> badgeDefinitionReputationEventCandidates = cacheBadgeDefinitionReputationEventServiceIF.getExistingReputationDefinitionEvents();

    List<AddressTag> formulasTarget = cacheFormulaEventServiceIF.getFormulaEventsGivenAssociatedBadgeDefinitionGenericEvent(badgeDefinitionAwardEvent).stream().map(FormulaEvent::asAddressTag).toList();

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

    List<FollowSetsEvent> awardRecipientFollowSets =
        cacheFollowSetsEventServiceIF.getEventsByPubkeyTag(awardRecipientPublicKey).stream()
            .filter(followSetsEvent ->
                matchingBadgeDefinitionReputationEvents.stream()
                    .map(BadgeDefinitionReputationEvent::getIdentifierTag)
                    .anyMatch(followSetsEvent.getIdentifierTag()::equals)).toList();

    List<FollowSetsEvent> followSetsEvents = awardRecipientFollowSets.stream().map(followSetsEvent ->
        createFollowSetsEvent(
            followSetsEvent.getTypeSpecificTags(PubKeyTag.class).stream().map(PubKeyTag::getPublicKey).findFirst().orElseThrow(),
            followSetsEvent.getIdentifierTag(),
            Stream.concat(
                    followSetsEvent.getBadgeAwardGenericEvents().stream(),
                    Stream.of(reconstructedVoteEvent))
                .toList())).toList();

    List<FollowSetsEvent> listToSend = !followSetsEvents.isEmpty() ? followSetsEvents :
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

    listToSend.stream().map(FollowSetsEvent::getGenericEventRecord).forEach(afterimageFollowSetsEventPlugin::processIncomingEvent);
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
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }
}
