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
import com.prosilion.nostr.tag.SetsPairedEvent;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
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
  private final Identity aImgIdentity;
  private final Relay relay;

  public AbstractVoteEventKindPlugin(
     @NonNull String afterimageRelayUrl,
     @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
     @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull AfterimageFollowSetsEventKindPlugin afterimageFollowSetsEventKindPlugin,
     @NonNull CacheFormulaEventServiceIF cacheFormulaEventServiceIF,
     @NonNull EventPlugin eventPlugin,
     @NonNull Identity aImgIdentity) {
    super(eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheBadgeDefinitionGenericEventService = cacheBadgeDefinitionGenericEventService;
    this.cacheBadgeDefinitionReputationEventService = cacheBadgeDefinitionReputationEventService;
    this.cacheFollowSetsEventService = cacheFollowSetsEventService;
    this.afterimageFollowSetsEventKindPlugin = afterimageFollowSetsEventKindPlugin;
    this.cacheFormulaEventServiceIF = cacheFormulaEventServiceIF;
    this.relay = new Relay(afterimageRelayUrl);
    log.debug("using afterimageRelayUrl: [{}]", afterimageRelayUrl);
  }

  @Override
  public Optional<GenericEventRecord> processIncomingEvent(@NonNull EventIF voteEvent, @NonNull Relay fromRelay) {
    log.debug("processing incoming voteEvent\n{}", voteEvent.createPrettyPrintJson());

    AddressTag voteEventAddressTagIsAwardDefnEvent = voteEvent.asGenericEventRecord().requireFirstTag(AddressTag.class);
    Relay consolidatedRelay = voteEventAddressTagIsAwardDefnEvent.findRelay().orElse(fromRelay);
    log.debug("processIncomingEvent(voteEvent, Relay):\n  voteEventAddressTagIsAwardDefnEvent Relay: [ {} ]\n  eventual consolidated Relay: [ {} ]",
       voteEventAddressTagIsAwardDefnEvent.findRelay().map(Relay::getUrl).orElse("NULL"),
       consolidatedRelay.getUrl());

    AddressTag addressTagIsAwardDefnEventReconstructed = new AddressTag(
       voteEventAddressTagIsAwardDefnEvent.getKind(),
       voteEventAddressTagIsAwardDefnEvent.getPublicKey(),
       voteEventAddressTagIsAwardDefnEvent.requireIdentifierTag(),
       consolidatedRelay);

    BadgeDefinitionGenericEvent badgeDefinitionUpvoteEvent = cacheBadgeDefinitionGenericEventService.getBy(addressTagIsAwardDefnEventReconstructed).orElseThrow(() ->
       new NostrException(
          String.format("no BadgeDefinitionUpvoteEvent matches incoming voteEvent:\n  %s", voteEvent.createPrettyPrintJson())));
    log.debug("(1of13V) badgeDefinitionUpvoteEvent:\n  {}", badgeDefinitionUpvoteEvent.createPrettyPrintJson());

    FormulaEvent formulaEvent = cacheFormulaEventServiceIF.getBy(badgeDefinitionUpvoteEvent.asAddressableEventAddressTag())
       .orElseThrow(() -> new NostrException(
          String.format("no formulaEvent matches badgeDefinitionUpvoteEvent.asAddressableEventAddressTag():\n  %s",
             badgeDefinitionUpvoteEvent.asAddressableEventAddressTag().toStringPrettyPrint())));
    log.debug("(3of13V) Optional<FormulaEvent> formulaEvent:\n  {}", formulaEvent.createPrettyPrintJson());

    PubKeyTag recipientPublicKeyAsPubKeyTag = voteEvent.requireFirstTag(PubKeyTag.class);
    AddressTag formulaEventAddressableEventAddressTag = formulaEvent.asAddressableEventAddressTag();

    log.debug("(4of13V) calling cacheBadgeDefinitionReputationEventService.getByDirectTag(addressTag):\n  {}", formulaEvent.createPrettyPrintJson());
    BadgeDefinitionReputationEvent existingDefnReputation =
       cacheBadgeDefinitionReputationEventService.getByDirectTag(formulaEventAddressableEventAddressTag).stream().findFirst().orElseThrow(() ->
          new NostrException(String.format("no BadgeDefinitionReputationEvent found for formulaEventAddressableEventAddressTag:\n  %s",
             formulaEventAddressableEventAddressTag.toStringPrettyPrint())));

    //  filter FollowSetsEvents containing matching badgeDefinitionReputationEvent 
    List<FollowSetsEvent> targetedFollowSets = cacheFollowSetsEventService
       .getBy(voteEvent.requireFirstTag(PubKeyTag.class), badgeDefinitionUpvoteEvent.asAddressableEventAddressTag())
       .stream().peek(followSetsEventPeek ->
          log.debug("(3of6) ... followSetsEventPeek:\n{}", followSetsEventPeek))
       .filter(followSetsEvent ->
          followSetsEvent.getBadgeSetsEventList().stream()
             .map(BadgeSetsEvent::getBadgeDefinitionReputationEvent).map(AddressableEvent::asAddressableEventAddressTag).toList()
             .contains(addressTagIsAwardDefnEventReconstructed)).toList();
    log.debug("(4of6) ... targetedFollowSets:\n{}", targetedFollowSets.stream()
       .map(EventIF::createPrettyPrintJson).collect(Collectors.joining(",\n  ")));

    SetsPairedEvent setsPairedEvents = new SetsPairedEvent(
       badgeDefinitionUpvoteEvent.asAddressableEventAddressTag(), // may contain NULL relay == defn couldn't be found == undependable defn
       fromRelay,
       new EventTag(voteEvent.getId(), consolidatedRelay.getUrl()),
       voteEvent.getPublicKey());
    log.debug("(5of6) ... adding setsPairedEvents:\n{}", setsPairedEvents);

    List<FollowSetsEvent> followSetsEventsToSend = new java.util.ArrayList<>(List.of());

    if (!targetedFollowSets.isEmpty()) {
      followSetsEventsToSend.addAll(targetedFollowSets.stream().map(followSetsEvent ->
         followSetsEvent.createNewFromExisting(aImgIdentity,
            followSetsEvent.getBadgeSetsEventList().stream().map(badgeSetsEvent ->
               badgeSetsEvent.createNewFromExisting(aImgIdentity,
                  new CurationSetsEvent(
                     aImgIdentity,
                     existingDefnReputation,
                     setsPairedEvents,
                     consolidatedRelay))).toList())).toList());
    } else {
      followSetsEventsToSend.add(
         new FollowSetsEvent(aImgIdentity,
            new BadgeSetsEvent(aImgIdentity,
               existingDefnReputation,
               new CurationSetsEvent(
                  aImgIdentity,
                  existingDefnReputation,
                  setsPairedEvents,
                  relay),
               relay),
            relay));
    }

    log.debug("(10of13V followSets to send:\n  {}", followSetsEventsToSend.isEmpty() ? "EMPTY FLATMAP" :
       followSetsEventsToSend.stream().map(EventIF::createPrettyPrintJson).collect(Collectors.joining(",\n  ")));

    log.debug("(12of13V) ... calling followSetsEventsToSend.stream().map(afterimageFollowSetsEventKindPlugin::processIncomingEvent) ...");
    followSetsEventsToSend.forEach(e -> afterimageFollowSetsEventKindPlugin.processIncomingEvent(e, fromRelay));

    log.debug("(13of13V) ... done.  returning upvoteEventReconstructed.asGenericEventRecord():\n  {}",
       Optional.of(voteEvent.asGenericEventRecord()).map(GenericEventRecord::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));
    return Optional.of(voteEvent.asGenericEventRecord());
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }
}
