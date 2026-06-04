package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

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
  }

  @Override
  public GenericEventRecord processIncomingEvent(@NonNull EventIF voteEvent) {
    log.debug("processing incoming voteEvent\n{}", voteEvent.createPrettyPrintJson());

    BadgeDefinitionGenericEvent badgeDefinitionUpvoteEvent = cacheBadgeDefinitionGenericEventService.getBy(voteEvent.asGenericEventRecord().requireFirstTag(AddressTag.class)).orElseThrow(() ->
        new NostrException(
            String.format("no BadgeDefinitionUpvoteEvent matches incoming voteEvent:\n  %s", voteEvent.createPrettyPrintJson())));
    log.debug("(1of13V) badgeDefinitionUpvoteEvent:\n  {}", badgeDefinitionUpvoteEvent.createPrettyPrintJson());

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> upvoteEventReconstructed =
        new BadgeAwardGenericEvent<>(voteEvent.asGenericEventRecord(), aTag -> badgeDefinitionUpvoteEvent);

    log.debug("(2of13V) upvoteEventReconstructed:\n  {}", upvoteEventReconstructed.createPrettyPrintJson());

    FormulaEvent formulaEvent = cacheFormulaEventServiceIF.getBy(badgeDefinitionUpvoteEvent.asAddressableEventAddressTag())
        .orElseThrow(() -> new NostrException(
            String.format("no formulaEvent matches badgeDefinitionUpvoteEvent.asAddressableEventAddressTag():\n  %s",
                badgeDefinitionUpvoteEvent.asAddressableEventAddressTag().toStringPrettyPrint())));
    log.debug("(3of13V) Optional<FormulaEvent> formulaEvent:\n  {}", formulaEvent.createPrettyPrintJson());

    PubKeyTag recipientPublicKeyAsPubKeyTag = voteEvent.requireFirstTag(PubKeyTag.class);
    AddressTag formulaEventAddressableEventAddressTag = formulaEvent.asAddressableEventAddressTag();

    List<BadgeDefinitionReputationEvent> byDirectTag = cacheBadgeDefinitionReputationEventService.getByDirectTag(formulaEventAddressableEventAddressTag);
    BadgeDefinitionReputationEvent existingReputationDefinitionEvent =
        byDirectTag.stream().findFirst().orElseThrow(() ->
            new NostrException(String.format("no BadgeDefinitionReputationEvent found for formulaEventAddressableEventAddressTag:\n  %s",
                formulaEventAddressableEventAddressTag.toStringPrettyPrint())));
    log.debug("(5of13V) existingReputationDefinitionEvent:\n  {}", existingReputationDefinitionEvent.createPrettyPrintJson());

    AddressTag existingReputationDefinitionEventAsAddressTag = existingReputationDefinitionEvent.asAddressableEventAddressTag();
    log.debug("(6of13V) calling cacheFollowSetsEventService.getBy(recipientPublicKeyAsPubKeyTag, addressTag):\n  [{}]\n  [{}]",
        recipientPublicKeyAsPubKeyTag.getPublicKey(),
        existingReputationDefinitionEventAsAddressTag);

    Optional<FollowSetsEvent> awardRecipientExistingFollowSets = cacheFollowSetsEventService.getBy(recipientPublicKeyAsPubKeyTag, existingReputationDefinitionEventAsAddressTag);

    log.debug("(7of13V) ... awardRecipientExistingFollowSets:\n{}", awardRecipientExistingFollowSets
        .map(EventIF::createPrettyPrintJson).orElse("no awardRecipientExistingFollowSets yet"));

    FollowSetsEvent followSetsEventToSend = createFollowSetsEvent(
        existingReputationDefinitionEvent,
        Stream.concat(
                awardRecipientExistingFollowSets
                    .stream()
                    .map(FollowSetsEvent::getBadgeAwardGenericEvents)
                    .flatMap(Collection::stream),
                Stream.of(upvoteEventReconstructed))
            .toList());
    log.debug("(9of13V) ... followSetsEventToSend:\n{}", followSetsEventToSend.createPrettyPrintJson());

    log.debug("(10of13V) ... cacheServiceIF.save(upvoteEventReconstructed) ...");
    cacheServiceIF.save(upvoteEventReconstructed);
    log.debug("(11of13V) ... saved ...");

    log.debug("(12of13V) ... calling followSetsEventToSend.stream().map(afterimageFollowSetsEventKindPlugin::processIncomingEvent) ...");
    GenericEventRecord unused = afterimageFollowSetsEventKindPlugin.processIncomingEvent(followSetsEventToSend);
    log.debug("(13of13V) ... done.  returning upvoteEventReconstructed.asGenericEventRecord():\n  {}", upvoteEventReconstructed.createPrettyPrintJson());
    return upvoteEventReconstructed.asGenericEventRecord();
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
