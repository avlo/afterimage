package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BadgeSetsEvent;
import com.prosilion.nostr.event.CuratedBadgeAwardGenericEvent;
import com.prosilion.nostr.event.CuratedBadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.ReferenceTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.curated.CacheCuratedBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.base.cache.CacheFormulaEventServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.NonPublishingEventKindPlugin;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

@Slf4j
// our SportsCar extends CarDecorator
public abstract class AbstractVoteEventKindPlugin extends NonPublishingEventKindPlugin {
  private final CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService;
  private final CacheCuratedBadgeDefinitionGenericEventService cacheCuratedBadgeDefinitionGenericEventService;
  private final CacheBadgeDefinitionReputationEventService badgeDefinitionReputationEventService;
  private final CacheFollowSetsEventService followSetsEventService;
  private final AfterimageFollowSetsEventKindPlugin followSetsEventKindPlugin;
  private final CacheFormulaEventServiceIF formulaEventService;
  private final Identity aImgIdentity;
  private final Relay relay;

  public AbstractVoteEventKindPlugin(
     @NonNull String afterimageRelayUrl,
     @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
     @NonNull CacheCuratedBadgeDefinitionGenericEventService cacheCuratedBadgeDefinitionGenericEventService,
     @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull AfterimageFollowSetsEventKindPlugin afterimageFollowSetsEventKindPlugin,
     @NonNull CacheFormulaEventServiceIF cacheFormulaEventServiceIF,
     @NonNull EventPlugin eventPlugin,
     @NonNull Identity aImgIdentity) {
    super(eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheBadgeDefinitionGenericEventService = cacheBadgeDefinitionGenericEventService;
    this.cacheCuratedBadgeDefinitionGenericEventService = cacheCuratedBadgeDefinitionGenericEventService;
    this.badgeDefinitionReputationEventService = cacheBadgeDefinitionReputationEventService;
    this.followSetsEventService = cacheFollowSetsEventService;
    this.followSetsEventKindPlugin = afterimageFollowSetsEventKindPlugin;
    this.formulaEventService = cacheFormulaEventServiceIF;
    this.relay = new Relay(afterimageRelayUrl);
    log.debug("using afterimageRelayUrl: [{}]", afterimageRelayUrl);
  }

  @Override
  public Optional<GenericEventRecord> processIncomingEvent(@NonNull EventIF voteEvent, @NonNull Relay fromRelay) {
    log.debug("processing incoming voteEvent\n{}", voteEvent.createPrettyPrintJson());

    PubKeyTag recipientPublicKeyAsPubKeyTag = voteEvent.requireFirstTag(PubKeyTag.class);
    Optional<Relay> awardEventRelay = voteEvent.getRelayTag().map(RelayTag::getRelay);
    Relay awardEventConsolidatedRelay = awardEventRelay.orElse(fromRelay);
    EventTag eventTag = new EventTag(voteEvent.getId(), awardEventConsolidatedRelay.getUrl());

    Optional<CuratedBadgeDefinitionGenericEvent> existingCurationSetsEvent = cacheCuratedBadgeDefinitionGenericEventService.getByDirect(eventTag);
    if (existingCurationSetsEvent.isPresent())
      return existingCurationSetsEvent.map(CuratedBadgeDefinitionGenericEvent::getGenericEventRecord);

    CuratedBadgeDefinitionGenericEvent curatedBadgeDefinitionEvent = new CuratedBadgeDefinitionGenericEvent(
       aImgIdentity,
       cacheBadgeDefinitionGenericEventService.getByExpanded(voteEvent.asGenericEventRecord().requireFirstTag(AddressTag.class)).orElseThrow(),
       new ReferenceTag(fromRelay.getUrl()),
       relay);

    log.debug("(2of13V) saving incoming vote as CuratedBadgeDefinitionGenericEvent:\n  {}", curatedBadgeDefinitionEvent.createPrettyPrintJson());
    super.processIncomingEvent(curatedBadgeDefinitionEvent, awardEventConsolidatedRelay);

    FormulaEvent formulaEvent = formulaEventService.getByDirect(curatedBadgeDefinitionEvent.getAddressTag())
       .orElseThrow(() -> new NostrException(
          String.format("no formulaEvent matches badgeDefinitionGenericEvent.asAddressableEventAddressTag():\n  %s",
             curatedBadgeDefinitionEvent.getAddressTag().toStringPrettyPrint())));
    log.debug("(3of13V) Optional<FormulaEvent> formulaEvent:\n  {}", formulaEvent.createPrettyPrintJson());
    super.processIncomingEvent(formulaEvent, relay);

    AddressTag formulaEventAddressableEventAddressTag = formulaEvent.asAddressableEventAddressTag();
    log.debug("(4of13V) calling cacheBadgeDefinitionReputationEventService.getByDirectTag(addressTag):\n  {}", formulaEvent.createPrettyPrintJson());
    BadgeDefinitionReputationEvent existingDefnReputation =
       badgeDefinitionReputationEventService.getByDirect(formulaEventAddressableEventAddressTag).stream().findFirst().orElseThrow(() ->
          new NostrException(String.format("no BadgeDefinitionReputationEvent found for formulaEventAddressableEventAddressTag:\n  %s",
             formulaEventAddressableEventAddressTag.toStringPrettyPrint())));

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardGenericEvent =
       new BadgeAwardGenericEvent<>(
          voteEvent.asGenericEventRecord(), addressTag -> existingDefnReputation);

    CuratedBadgeAwardGenericEvent curatedBadgeAwardEvent = new CuratedBadgeAwardGenericEvent(
       aImgIdentity,
       badgeAwardGenericEvent,
       curatedBadgeDefinitionEvent,
       new ReferenceTag(fromRelay.getUrl()),
       fromRelay);

    BadgeSetsEvent badgeSetsEvent = new BadgeSetsEvent(aImgIdentity, existingDefnReputation, curatedBadgeAwardEvent, relay);
    super.processIncomingEvent(badgeSetsEvent, relay);
    FollowSetsEvent followSetsEvent = new FollowSetsEvent(aImgIdentity,
       badgeSetsEvent,
       relay);

    followSetsEventKindPlugin.processIncomingEvent(followSetsEvent, awardEventConsolidatedRelay);

    log.debug("(13of13V) ... done.  returning upvoteEventReconstructed.asGenericEventRecord():\n  {}",
       Optional.of(voteEvent.asGenericEventRecord()).map(GenericEventRecord::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));
    return Optional.of(voteEvent.asGenericEventRecord());
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }
}
