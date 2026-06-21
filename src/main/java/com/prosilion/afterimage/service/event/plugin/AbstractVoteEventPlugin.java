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
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.base.service.event.CacheFollowSetsEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.definition.CacheBadgeDefinitionReputationEventService;
import com.prosilion.superconductor.base.cache.CacheFormulaEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.NonPublishingEventKindPlugin;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// our SportsCar extends CarDecorator
public abstract class AbstractVoteEventPlugin extends NonPublishingEventKindPlugin {
  private final CacheServiceIF cacheServiceIF;
  private final CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService;
  private final CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService;
  private final CacheFollowSetsEventService cacheFollowSetsEventService;
  private final AfterimageFollowSetsEventKindPlugin afterimageFollowSetsEventKindPlugin;
  private final CacheFormulaEventServiceIF cacheFormulaEventServiceIF;
  private final Identity aImgIdentity;
  private final Relay relay;

  public AbstractVoteEventPlugin(
     @NonNull String afterimageRelayUrl,
     @NonNull CacheServiceIF cacheServiceIF,
     @NonNull CacheBadgeDefinitionGenericEventService cacheBadgeDefinitionGenericEventService,
     @NonNull CacheBadgeDefinitionReputationEventService cacheBadgeDefinitionReputationEventService,
     @NonNull CacheFollowSetsEventService cacheFollowSetsEventService,
     @NonNull AfterimageFollowSetsEventKindPlugin afterimageFollowSetsEventKindPlugin,
     @NonNull CacheFormulaEventServiceIF cacheFormulaEventServiceIF,
     @NonNull EventPlugin eventPlugin,
     @NonNull Identity aImgIdentity) {
    super(eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.cacheBadgeDefinitionGenericEventService = cacheBadgeDefinitionGenericEventService;
    this.cacheBadgeDefinitionReputationEventService = cacheBadgeDefinitionReputationEventService;
    this.cacheFollowSetsEventService = cacheFollowSetsEventService;
    this.afterimageFollowSetsEventKindPlugin = afterimageFollowSetsEventKindPlugin;
    this.cacheFormulaEventServiceIF = cacheFormulaEventServiceIF;
    this.relay = new Relay(afterimageRelayUrl);
    log.debug("using afterimageRelayUrl: [{}]", afterimageRelayUrl);
  }

  @Override
  public Optional<GenericEventRecord> processIncomingEvent(@NonNull EventIF voteEvent, @NonNull Relay relay) {
    log.debug("processing incoming voteEvent\n{}", voteEvent.createPrettyPrintJson());

    AddressTag voteEventAddressTag = voteEvent.asGenericEventRecord().requireFirstTag(AddressTag.class);
    Relay consolidatedRelay = voteEventAddressTag.findRelay().orElse(relay);

    log.debug("processIncomingEvent(voteEvent, Relay):\n  " +
          "voteEventAddressTag Relay: [ {} ]\n  " +
          "eventual consolidated Relay: [ {} ]",
       voteEventAddressTag.findRelay().map(Relay::getUrl).orElse("NULL"),
       consolidatedRelay.getUrl());

    AddressTag addressTag = new AddressTag(
       voteEventAddressTag.getKind(),
       voteEventAddressTag.getPublicKey(),
       voteEventAddressTag.requireIdentifierTag(),
       consolidatedRelay);

    Optional<BadgeDefinitionGenericEvent> badgeDefinitionUpvoteEvent = cacheBadgeDefinitionGenericEventService.getBy(addressTag);
    log.debug("(1of13V) badgeDefinitionUpvoteEvent:\n  {}", badgeDefinitionUpvoteEvent
       .map(EventIF::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));

    Optional<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> upvoteEventReconstructed =
       badgeDefinitionUpvoteEvent.map(ev ->
          emptyAddressTagRelayTriesSourceRelay(
             voteEvent, addressTag, ev));

    log.debug("(2of13V) upvoteEventReconstructed:\n  {}", upvoteEventReconstructed.map(EventIF::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));

    Optional<FormulaEvent> formulaEvent = badgeDefinitionUpvoteEvent.map(AddressableEvent::asAddressableEventAddressTag).flatMap(cacheFormulaEventServiceIF::getBy);

    log.debug("(3of13V) Optional<FormulaEvent> formulaEvent:\n  {}", formulaEvent.map(EventIF::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));

    Optional<AddressTag> formulaEventAddressableEventAddressTag = formulaEvent.map(AddressableEvent::asAddressableEventAddressTag);

    Optional<BadgeDefinitionReputationEvent> existingReputationDefinitionEvent =
       formulaEventAddressableEventAddressTag.flatMap(cacheBadgeDefinitionReputationEventService::getByDirectTag).stream().findFirst();

    log.debug("(5of13V) existingReputationDefinitionEvent:\n  {}", existingReputationDefinitionEvent.map(EventIF::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));

    Optional<FollowSetsEvent> awardRecipientExistingFollowSets = existingReputationDefinitionEvent.map(AddressableEvent::asAddressableEventAddressTag)
       .flatMap(cacheFollowSetsEventService::getBy);

    log.debug("(7of13V) ... awardRecipientExistingFollowSets:\n{}", awardRecipientExistingFollowSets
       .map(EventIF::createPrettyPrintJson).orElse("no awardRecipientExistingFollowSets yet"));

    Optional<FollowSetsEvent> followSetsEventToSend = existingReputationDefinitionEvent.map(ev ->
       createFollowSetsEvent(
          ev,
          Stream.concat(
                awardRecipientExistingFollowSets
                   .map(FollowSetsEvent::getBadgeAwardGenericEvents)
                   .stream().flatMap(Collection::stream),
                upvoteEventReconstructed.stream())
             .toList()));

    log.debug("(9of13V) ... followSetsEventToSend:\n{}", followSetsEventToSend.map(EventIF::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));

    log.debug("(10of13V) ... cacheServiceIF.save(upvoteEventReconstructed) ...");

    upvoteEventReconstructed.map(cacheServiceIF::save);
    log.debug("(11of13V) ... saved ...");

    log.debug("(12of13V) ... calling followSetsEventToSend.stream().map(afterimageFollowSetsEventKindPlugin::processIncomingEvent) ...");

    followSetsEventToSend.map(e -> afterimageFollowSetsEventKindPlugin.processIncomingEvent(e, relay));

    log.debug("(13of13V) ... done.  returning upvoteEventReconstructed.asGenericEventRecord():\n  {}",
       upvoteEventReconstructed.map(EventIF::createPrettyPrintJson).orElse("EMPTY OPTIONAL"));
    return upvoteEventReconstructed.map(EventIF::asGenericEventRecord);
  }

  //  TODO: public only for test, consider rxr
  public static BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> emptyAddressTagRelayTriesSourceRelay(
     EventIF event,
     AddressTag addressTag,
     BadgeDefinitionGenericEvent badgeDefinitionUpvoteEvent) {
    return new BadgeAwardGenericEvent<>(
       new GenericEventRecord(
          event.getId(),
          event.getPublicKey(),
          event.getCreatedAt(),
          event.getKind(),
          List.of(new AddressTag(
                addressTag.getKind(),
                addressTag.getPublicKey(),
                addressTag.requireIdentifierTag(),
                addressTag.requireRelay()),
             new RelayTag(addressTag.requireRelay()),
             event.requireFirstTag(PubKeyTag.class)),
          event.getContent(),
          event.getSignature()), aTag -> badgeDefinitionUpvoteEvent);
  }

  private FollowSetsEvent createFollowSetsEvent(
     @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent,
     @NonNull List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> badgeAwardGenericVoteEvent) {
    return new FollowSetsEvent(
       aImgIdentity,
       badgeDefinitionReputationEvent,
       relay,
       badgeAwardGenericVoteEvent);
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }
}
