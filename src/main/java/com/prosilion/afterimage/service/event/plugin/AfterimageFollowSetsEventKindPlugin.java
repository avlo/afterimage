package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.calculator.DynamicReputationCalculator;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.cache.CacheFollowSetsEventServiceIF;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.cache.tag.CacheKindAddressTagServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.PublishingEventKindPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.request.subscriber.NotifierService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageFollowSetsEventKindPlugin extends PublishingEventKindPlugin { // kind 30_000
  private final Identity aImgIdentity;
  private final CacheServiceIF cacheServiceIF;
  private final CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF;
  private final CacheKindAddressTagServiceIF cacheKindAddressTagServiceIF;
  private final EventKindTypePluginIF badgeAwardReputationEventKindTypePlugin;
  private final Relay relay;

  public AfterimageFollowSetsEventKindPlugin(
     @NonNull String afterimageRelayUrl,
     @NonNull NotifierService notifierService,
     @NonNull EventPlugin eventPlugin,
     @NonNull CacheServiceIF cacheServiceIF,
     @NonNull CacheFollowSetsEventServiceIF cacheFollowSetsEventServiceIF,
     @NonNull CacheKindAddressTagServiceIF cacheKindAddressTagServiceIF,
     @NonNull Identity aImgIdentity,
     @NonNull EventKindTypePluginIF badgeAwardReputationEventKindTypePlugin) {
    super(notifierService, eventPlugin);
    this.aImgIdentity = aImgIdentity;
    this.cacheServiceIF = cacheServiceIF;
    this.cacheFollowSetsEventServiceIF = cacheFollowSetsEventServiceIF;
    this.cacheKindAddressTagServiceIF = cacheKindAddressTagServiceIF;
    this.badgeAwardReputationEventKindTypePlugin = badgeAwardReputationEventKindTypePlugin;
    this.relay = new Relay(afterimageRelayUrl);
  }

  @Override
  public GenericEventRecord processIncomingEvent(@NonNull EventIF incomingFollowSetsEvent) {
    FollowSetsEvent materializedFollowSetsEvent = cacheFollowSetsEventServiceIF.materialize(incomingFollowSetsEvent);
    log.debug("materializedFollowSetsEvent:\n{}", materializedFollowSetsEvent.createPrettyPrintJson());

    PublicKey upvotedUserPubKeyTagPublicKey = materializedFollowSetsEvent.getAwardRecipientPulicKey();
    Optional<GenericEventRecord> existingFollowSetsEventGEROpt = cacheKindAddressTagServiceIF.getBy(
       Kind.FOLLOW_SETS,
       new PubKeyTag(upvotedUserPubKeyTagPublicKey),
       materializedFollowSetsEvent.getAddressTag()).stream().findFirst();
    log.debug("(5ofX) ... existingFollowSetsEventGEROpt:\n  {}",
       existingFollowSetsEventGEROpt
          .map(genericEventRecord -> genericEventRecord.createPrettyPrintJson())
          .orElse("EMPTY existingFollowSetsEventGEROpt OPTIONAL"));

    Optional<FollowSetsEvent> existingFollowSetsEventOpt = existingFollowSetsEventGEROpt.flatMap(event ->
       cacheFollowSetsEventServiceIF.getEvent(
          event.getId(),
          event.getTypeSpecificTags(RelayTag.class).getFirst().getRelay().getUrl()));
    log.debug("(5ofX) ... existingFollowSetsEventOpt:\n  {}",
       existingFollowSetsEventOpt
          .map(EventIF::createPrettyPrintJson)
          .orElse("EMPTY existingFollowSetsEventOpt OPTIONAL"));

    List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> existingVoteEvents = existingFollowSetsEventOpt.stream().map(FollowSetsEvent::getBadgeAwardGenericEvents).flatMap(Collection::stream).toList();
    log.debug("(6ofX) ... existingFollowSetsEventOpt:\n  {}", existingVoteEvents.stream().map(EventIF::createPrettyPrintJson));

    List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> nonMatchingVoteEvents =
       materializedFollowSetsEvent.getBadgeAwardGenericEvents().stream()
          .filter(incomingBadgeAwardVoteEvent ->
             !existingVoteEvents.contains(incomingBadgeAwardVoteEvent)).toList();
    log.debug("(7ofX) ... nonMatchingVoteEvents:");
    nonMatchingVoteEvents.stream().map(EventIF::createPrettyPrintJson).forEach(log::debug);
    log.debug("end (7ofX) debug stmts");

    FollowSetsEvent notifierFollowSetsEvent = createFollowSetsEvent(
       materializedFollowSetsEvent.getBadgeDefinitionReputationEvent(),
       Stream.concat(
          existingVoteEvents.stream(),
          nonMatchingVoteEvents.stream()).toList());
    log.debug("(8ofX) ... notifierFollowSetsEvent:\n  {}", notifierFollowSetsEvent.createPrettyPrintJson());

    existingFollowSetsEventOpt.ifPresent(this::deletePreviousFollowSetsEvent);
    super.processIncomingEvent(notifierFollowSetsEvent);
    log.debug("(9ofX) super.processIncomingEvent(notifierFollowSetsEvent) called...");

    FollowSetsEvent followsSetAsReputationEvent = createFollowSetsEvent(
       materializedFollowSetsEvent.getBadgeDefinitionReputationEvent(),
       nonMatchingVoteEvents);
    log.debug("(10ofX) ... createFollowSetsEvent(...) method successfully created followsSetAsReputationEvent:\n  {}", followsSetAsReputationEvent.createPrettyPrintJson());
    GenericEventRecord genericEventRecord = badgeAwardReputationEventKindTypePlugin.processIncomingEvent(followsSetAsReputationEvent);

    log.debug("(11ofX) super.processIncomingEvent(notifierFollowSetsEvent) completed, returned genericEventRecord:\n  {}", genericEventRecord.createPrettyPrintJson());
    return genericEventRecord;
  }

  private FollowSetsEvent createFollowSetsEvent(
     @NonNull BadgeDefinitionReputationEvent badgeDefinitionReputationEvent,
     @NonNull List<BadgeAwardGenericEvent<BadgeDefinitionGenericEvent>> badgeAwardGenericVoteEvents) {
    FollowSetsEvent followSetsEvent = new FollowSetsEvent(
       aImgIdentity,
       badgeDefinitionReputationEvent,
       relay,
       badgeAwardGenericVoteEvents,
       DynamicReputationCalculator.class.getSimpleName());
    log.debug("createFollowSetsEvent() created FollowSetsEvent: {}", followSetsEvent.createPrettyPrintJson());
    return followSetsEvent;
  }

  private void deletePreviousFollowSetsEvent(FollowSetsEvent previousFollowSetsEvent) {
    cacheServiceIF.deleteEvent(
       new DeletionEvent(
          aImgIdentity,
          List.of(new EventTag(
             previousFollowSetsEvent.getId(),
             previousFollowSetsEvent.requireRelayTagUrl())), "aImg delete previous FOLLOW_SETS event"));
  }

  @Override
  public Kind getKind() {
    log.debug("getKind Kind[{}]: {}",
       Kind.FOLLOW_SETS.getValue(),
       Kind.FOLLOW_SETS.getName().toUpperCase());
    return Kind.FOLLOW_SETS;
  }
}
