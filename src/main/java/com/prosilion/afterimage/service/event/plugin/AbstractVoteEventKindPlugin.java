package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.AddressableEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BadgeSetsEvent;
import com.prosilion.nostr.event.CurationSetsEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.base.cache.CacheCurationSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheFormulaEventServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.NonPublishingEventKindPlugin;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// our SportsCar extends CarDecorator
public abstract class AbstractVoteEventKindPlugin extends NonPublishingEventKindPlugin {
  private final CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService;
  private final CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService;
  private final CacheFollowSetsEventService cacheFollowSetsEventService;
  private final AfterimageFollowSetsEventKindPlugin afterimageFollowSetsEventKindPlugin;
  private final CacheFormulaEventServiceIF cacheFormulaEventServiceIF;
  private final CacheCurationSetsEventServiceIF cacheCurationSetsEventServiceIF;
  private final Identity aImgIdentity;
  private final Relay relay;

  public AbstractVoteEventKindPlugin(
     @NonNull String afterimageRelayUrl,
     @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
     @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull AfterimageFollowSetsEventKindPlugin afterimageFollowSetsEventKindPlugin,
     @NonNull CacheFormulaEventServiceIF cacheFormulaEventServiceIF,
     @NonNull CacheCurationSetsEventServiceIF cacheCurationSetsEventServiceIF,
     @NonNull EventPlugin eventPlugin,
     @NonNull Identity aImgIdentity) {
    super(eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheBadgeDefinitionGenericEventService = cacheBadgeDefinitionGenericEventService;
    this.cacheBadgeDefinitionReputationEventService = cacheBadgeDefinitionReputationEventService;
    this.cacheFollowSetsEventService = cacheFollowSetsEventService;
    this.afterimageFollowSetsEventKindPlugin = afterimageFollowSetsEventKindPlugin;
    this.cacheFormulaEventServiceIF = cacheFormulaEventServiceIF;
    this.cacheCurationSetsEventServiceIF = cacheCurationSetsEventServiceIF;
    this.relay = new Relay(afterimageRelayUrl);
    log.debug("using afterimageRelayUrl: [{}]", afterimageRelayUrl);
  }

  @Override
  public Optional<GenericEventRecord>   processIncomingEvent(@NonNull EventIF voteEvent, @NonNull Relay fromRelay) {
  log.debug("processing incoming voteEvent\n{}", voteEvent.createPrettyPrintJson());

    PubKeyTag recipientPublicKeyAsPubKeyTag = voteEvent.requireFirstTag(PubKeyTag.class);
    Optional<Relay> awardEventRelay = voteEvent.getRelayTag().map(RelayTag::getRelay);
    Relay awardEventConsolidatedRelay = awardEventRelay.orElse(fromRelay);
    EventTag eventTag = new EventTag(voteEvent.getId(), awardEventConsolidatedRelay.getUrl());

    Optional<CurationSetsEvent> existingCurationSetsEvent = cacheCurationSetsEventServiceIF.getBy(recipientPublicKeyAsPubKeyTag, eventTag);
    if (existingCurationSetsEvent.isPresent())
      return existingCurationSetsEvent.map(CurationSetsEvent::getGenericEventRecord);

    AddressTag voteEventAddressTagIsAwardDefnEvent = voteEvent.asGenericEventRecord().requireFirstTag(AddressTag.class);
    Relay awardDefnRelayOtherwiseFallbackRelay = voteEventAddressTagIsAwardDefnEvent.findRelay().orElse(awardEventConsolidatedRelay);
    log.debug("processIncomingEvent(voteEvent, Relay):\n  voteEventAddressTagIsAwardDefnEvent Relay: [ {} ]\n  eventual consolidated Relay: [ {} ]",
       voteEventAddressTagIsAwardDefnEvent.findRelay().map(Relay::getUrl).orElse("NULL"),
       awardDefnRelayOtherwiseFallbackRelay.getUrl());

    AddressTag addressTagBestAttempt = new AddressTag(
       voteEventAddressTagIsAwardDefnEvent.getKind(),
       voteEventAddressTagIsAwardDefnEvent.getPublicKey(),
       voteEventAddressTagIsAwardDefnEvent.requireIdentifierTag(),
       awardDefnRelayOtherwiseFallbackRelay);

    AddressTag dbAwardDefnEventAddressTagBestAttempt = cacheBadgeDefinitionGenericEventService
       .getBy(addressTagBestAttempt)
       .map(BadgeDefinitionGenericEvent::asAddressableEventAddressTag)
       .orElse(voteEventAddressTagIsAwardDefnEvent);
    log.debug("(1of13V) dbAwardDefnEventAddressTagBestAttempt:\n  {}", dbAwardDefnEventAddressTagBestAttempt);

    CurationSetsEvent curationSetsEvent = new CurationSetsEvent(
       aImgIdentity,
       recipientPublicKeyAsPubKeyTag.publicKey(),
       dbAwardDefnEventAddressTagBestAttempt.getIdentifierTag(),
       dbAwardDefnEventAddressTagBestAttempt,
       eventTag,
       relay);

    log.debug("(2of13V) saving incoming vote as CurationSetsEvent:\n  {}", curationSetsEvent.createPrettyPrintJson());
    super.processIncomingEvent(curationSetsEvent, awardEventConsolidatedRelay);

    FormulaEvent formulaEvent = cacheFormulaEventServiceIF.getBy(dbAwardDefnEventAddressTagBestAttempt)
       .orElseThrow(() -> new NostrException(
          String.format("no formulaEvent matches dbAwardDefnEventAddressTagBestAttempt.asAddressableEventAddressTag():\n  %s",
             dbAwardDefnEventAddressTagBestAttempt.toStringPrettyPrint())));
    log.debug("(3of13V) Optional<FormulaEvent> formulaEvent:\n  {}", formulaEvent.createPrettyPrintJson());

    AddressTag formulaEventAddressableEventAddressTag = formulaEvent.asAddressableEventAddressTag();
    log.debug("(4of13V) calling cacheBadgeDefinitionReputationEventService.getByDirectTag(addressTag):\n  {}", formulaEvent.createPrettyPrintJson());
    BadgeDefinitionReputationEvent existingDefnReputation =
       cacheBadgeDefinitionReputationEventService.getByDirectTag(formulaEventAddressableEventAddressTag).stream().findFirst().orElseThrow(() ->
          new NostrException(String.format("no BadgeDefinitionReputationEvent found for formulaEventAddressableEventAddressTag:\n  %s",
             formulaEventAddressableEventAddressTag.toStringPrettyPrint())));

    //  filter FollowSetsEvents containing matching badgeDefinitionReputationEvent 
    List<FollowSetsEvent> targetedFollowSets = cacheFollowSetsEventService
       .getBy(voteEvent.requireFirstTag(PubKeyTag.class), dbAwardDefnEventAddressTagBestAttempt)
       .stream().peek(followSetsEventPeek ->
          log.debug("(3of6) ... followSetsEventPeek:\n{}", followSetsEventPeek))
       .filter(followSetsEvent ->
          followSetsEvent.getBadgeSetsEventList().stream()
             .map(BadgeSetsEvent::getBadgeDefinitionReputationEvent)
             .map(AddressableEvent::asAddressableEventAddressTag).toList()
             .contains(addressTagBestAttempt)).toList();
    log.debug("(4of6) ... targetedFollowSets:\n{}", targetedFollowSets.stream()
       .map(EventIF::createPrettyPrintJson).collect(Collectors.joining(",\n  ")));

    List<FollowSetsEvent> followSetsEventsToSend = new java.util.ArrayList<>(List.of());
    if (!targetedFollowSets.isEmpty()) {
      followSetsEventsToSend.addAll(targetedFollowSets.stream().map(followSetsEvent ->
         followSetsEvent.createNewFromExisting(aImgIdentity,
            followSetsEvent.getBadgeSetsEventList().stream().map(badgeSetsEvent ->
            {
              BadgeSetsEvent newBadgeSetsEventFromExisting = badgeSetsEvent.createNewFromExisting(aImgIdentity, curationSetsEvent);
              super.processIncomingEvent(newBadgeSetsEventFromExisting, relay);
              return newBadgeSetsEventFromExisting;
            }).toList())).toList());
    } else {
      BadgeSetsEvent badgeSetsEvent = new BadgeSetsEvent(aImgIdentity, existingDefnReputation, curationSetsEvent, relay);
      super.processIncomingEvent(badgeSetsEvent, relay);
      followSetsEventsToSend.add(
         new FollowSetsEvent(aImgIdentity,
            badgeSetsEvent,
            relay));
    }

    log.debug("(10of13V followSets to send:\n  {}", followSetsEventsToSend.isEmpty() ? "EMPTY FLATMAP" :
       followSetsEventsToSend.stream().map(EventIF::createPrettyPrintJson).collect(Collectors.joining(",\n  ")));

    log.debug("(12of13V) ... calling followSetsEventsToSend.stream().map(afterimageFollowSetsEventKindPlugin::processIncomingEvent) ...");
    followSetsEventsToSend.forEach(e -> afterimageFollowSetsEventKindPlugin.processIncomingEvent(e, awardEventConsolidatedRelay));

    log.debug("(13of13V) ... done.  returning upvoteEventReconstructed.asGenericEventRecord():\n  {}",
       Optional.of(voteEvent.asGenericEventRecord()).map(GenericEventRecord::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));
    return Optional.of(voteEvent.asGenericEventRecord());
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }
}
