package com.prosilion.afterimage.service.event.plugin.sc;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardCanonicalEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.curated.CuratedBadgeAwardCanonicalEvent;
import com.prosilion.nostr.event.curated.CuratedBadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.ReferenceTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.curation.service.CacheCuratedBadgeDefinitionGenericEventServiceIF;
import com.prosilion.superconductor.autoconfigure.curation.service.event.award.CacheCuratedBadgeAwardCanonicalEventService;
import com.prosilion.superconductor.autoconfigure.curation.service.event.sets.CacheFollowSetsEventService;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPluginIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.NonPublishingEventKindPlugin;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
// our SportsCar extends CarDecorator
public class UniversalVoteEventKindPlugin extends NonPublishingEventKindPlugin {
  private final CacheCuratedBadgeDefinitionGenericEventServiceIF cacheCuratedBadgeDefinitionGenericEventServiceIF;
  private final FollowSetsEventKindPlugin followSetsEventKindPlugin;
  private final Identity superconductorInstanceIdentity;
  private final Relay superconductorRelay;
  private final CacheCuratedBadgeAwardCanonicalEventService cacheCuratedBadgeAwardCanonicalEventService;
  private final CacheFollowSetsEventService cacheFollowSetsEventService;
  CacheServiceIF cacheServiceIF;

  public UniversalVoteEventKindPlugin(
     @NonNull String superconductorRelayUrl,
     @NonNull CacheCuratedBadgeDefinitionGenericEventServiceIF cacheCuratedBadgeDefinitionGenericEventServiceIF,
     @NonNull FollowSetsEventKindPlugin followSetsEventKindPlugin,
     @NonNull EventPluginIF eventPluginIF,
     @NonNull Identity superconductorInstanceIdentity,
     @NonNull CacheCuratedBadgeAwardCanonicalEventService cacheCuratedBadgeAwardCanonicalEventService,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     CacheServiceIF cacheServiceIF) {
    super(eventPluginIF);
    this.superconductorInstanceIdentity = superconductorInstanceIdentity;
    this.cacheCuratedBadgeDefinitionGenericEventServiceIF = cacheCuratedBadgeDefinitionGenericEventServiceIF;
    this.followSetsEventKindPlugin = followSetsEventKindPlugin;
    this.superconductorRelay = new Relay(superconductorRelayUrl);
    this.cacheCuratedBadgeAwardCanonicalEventService = cacheCuratedBadgeAwardCanonicalEventService;
    this.cacheFollowSetsEventService = cacheFollowSetsEventService;
    this.cacheServiceIF = cacheServiceIF;
    log.debug("using superconductorRelayUrl: [{}]", superconductorRelayUrl);
  }

  @Override
  public Optional<GenericEventRecord> processIncomingEvent(@NonNull EventIF incomingVoteEvent, @NonNull Relay fromRelay) {
    log.info("processIncomingEvent(incomingVoteEvent)\n{}", incomingVoteEvent.createPrettyPrintJson());

    Optional<CuratedBadgeAwardCanonicalEvent> curatedBadgeAwardCanonicalEventOpt =
       cacheCuratedBadgeAwardCanonicalEventService.getByDirect(
          new EventTag(
             incomingVoteEvent.getId(),
             incomingVoteEvent.findFirstTag(RelayTag.class).map(RelayTag::getRelay).map(Relay::getUrl).orElse(fromRelay.getUrl())));

    if (curatedBadgeAwardCanonicalEventOpt.isPresent()) {
      log.debug("curatedBadgeAwardCanonicalEvent exists, returning existing FollowSetsEvent");
      return cacheFollowSetsEventService.getBy(incomingVoteEvent.getTypeSpecificTags(PubKeyTag.class).getFirst()).stream().findFirst().map(EventIF::asGenericEventRecord);
    }

    AddressTag suppliedAddressTag = incomingVoteEvent.requireFirstTag(AddressTag.class);
    log.debug("calling cacheCuratedBadgeDefinitionGenericEventServiceIF.getByDirect(suppliedAddressTag, relayTag(nullable), fromRelay):\n  {}", suppliedAddressTag);

    CuratedBadgeDefinitionGenericEvent curatedBadgeDefinitionGenericEvent =
       cacheCuratedBadgeDefinitionGenericEventServiceIF.getByDirect(
          suppliedAddressTag,
          incomingVoteEvent.getRelayTag(),
          fromRelay).orElseThrow();
    log.debug("getByDirect(...) returned curatedBadgeDefinitionGenericEvent:\n  {}", curatedBadgeDefinitionGenericEvent.createPrettyPrintJson());

    BadgeAwardCanonicalEvent reconstructedVoteEvent =
       new BadgeAwardCanonicalEvent(
          incomingVoteEvent.asGenericEventRecord(),
          addressTag -> curatedBadgeDefinitionGenericEvent.getBadgeDefinitionGenericEvent());
    log.debug("reconstructedVoteEvent:\n  {}", reconstructedVoteEvent.createPrettyPrintJson());

    CuratedBadgeAwardCanonicalEvent curatedBadgeAwardCanonicalEvent =
       new CuratedBadgeAwardCanonicalEvent(
          superconductorInstanceIdentity,
          reconstructedVoteEvent,
          curatedBadgeDefinitionGenericEvent,
          new ReferenceTag(fromRelay.getUrl()),
          superconductorRelay);
    log.debug("constructed new CuratedBadgeAwardCanonicalEvent:\n  {}", curatedBadgeAwardCanonicalEvent.createPrettyPrintJson());

    super.processIncomingEvent(curatedBadgeAwardCanonicalEvent, fromRelay);
    log.debug("completed super.processIncomingEvent(curatedBadgeAwardCanonicalEvent, fromRelay)");

    log.debug("calling followSetsEventKindPlugin.processIncomingCuratedBadgeAwardCanonicalEvent(curatedBadgeAwardCanonicalEvent, fromRelay)");
    Optional<GenericEventRecord> genericEventRecord = followSetsEventKindPlugin.processIncomingCuratedBadgeAwardCanonicalEvent(curatedBadgeAwardCanonicalEvent, fromRelay);
    log.debug("completed. returning genericEventRecord");
    return genericEventRecord;
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }
}
