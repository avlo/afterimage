package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.calculator.DynamicReputationCalculator;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.EventTagAddressTagPair;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.service.GenericEventKind;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindPlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageFollowSetsEventPlugin extends PublishingEventKindPlugin { // kind 30_000
  private final EventKindTypePluginIF reputationEventPlugin;
  private final CacheServiceIF cacheServiceIF;
  private final Identity aImgIdentity;

  public AfterimageFollowSetsEventPlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull CacheServiceIF cacheServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull EventKindTypePluginIF reputationEventPlugin) {
    super(notifierService, eventKindPlugin);
    this.reputationEventPlugin = reputationEventPlugin;
    this.cacheServiceIF = cacheServiceIF;
    this.aImgIdentity = aImgIdentity;
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF incomingFollowSetsEvent) {
    log.debug("{}} processing incoming Kind.FOLLOW_SETS 30_000 : [{}]", getClass().getSimpleName(), incomingFollowSetsEvent);
    PublicKey voteReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, incomingFollowSetsEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();

    IdentifierTag identifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, incomingFollowSetsEvent)
        .stream()
        .findFirst().orElseThrow();

    Optional<GenericEventKind> existingFollowSetsEvent = getExistingFollowSetsEvent(voteReceiverPubkey, identifierTag);
//    TODO: ifPresent likely superfluous if delete mechanism already handles optional
    existingFollowSetsEvent.ifPresent(this::deletePreviousFollowSetsEvent);

    List<EventTagAddressTagPair> incomingPairs = getEventTagAddressTagPairs(incomingFollowSetsEvent.getTags());
    List<EventTagAddressTagPair> existingPairs = getEventTagAddressTagPairs(
        existingFollowSetsEvent.map(GenericEventKind::getTags).orElse(List.of()));

    List<EventTagAddressTagPair> nonMatches = incomingPairs.stream()
        .filter(incomingEventTagAddressTagPair ->
            !existingPairs.contains(incomingEventTagAddressTagPair)).toList();

    EventIF saveToDbEvent = createFollowSetsEvent(
        voteReceiverPubkey,
        identifierTag,
        Stream.concat(existingPairs.stream(), nonMatches.stream()).toList());
    super.processIncomingEvent(saveToDbEvent);

    EventIF createdReputationEvent = createFollowSetsEvent(
        voteReceiverPubkey,
        identifierTag,
        nonMatches);
    reputationEventPlugin.processIncomingEvent(createdReputationEvent);
  }

  private Optional<GenericEventKind> getExistingFollowSetsEvent(
      PublicKey badgeReceiverPubkey,
      IdentifierTag uuid) {
    return cacheServiceIF
        .getEventsByKindAndPubKeyTag(Kind.FOLLOW_SETS, badgeReceiverPubkey)
        .stream()
        .filter(eventIf ->
            Filterable.getTypeSpecificTags(IdentifierTag.class, eventIf)
                .contains(uuid))
        .max(Comparator.comparing(EventIF::getCreatedAt))
        .map(eventIF ->
            new GenericEventKind(
                eventIF.getId(),
                eventIF.getPublicKey(),
                eventIF.getCreatedAt(),
                eventIF.getKind(),
                eventIF.getTags(),
                eventIF.getContent(),
                eventIF.getSignature()));
  }

  public EventIF createFollowSetsEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull IdentifierTag identifierTag,
      @NonNull List<EventTagAddressTagPair> eventTagAddressTagPairs) {
    FollowSetsEvent followSetsEvent = new FollowSetsEvent(
        aImgIdentity,
        voteReceiverPubkey,
        identifierTag,
        eventTagAddressTagPairs,
        DynamicReputationCalculator.class.getSimpleName());

    GenericEventRecord genericEventRecord = followSetsEvent.getGenericEventRecord();

    return genericEventRecord;
  }

  public List<EventTagAddressTagPair> getEventTagAddressTagPairs(List<BaseTag> followSetsEvent) {
    List<EventTag> eventTags = followSetsEvent
        .stream()
        .filter(EventTag.class::isInstance)
        .map(EventTag.class::cast)
        .toList();

    List<AddressTag> addressTags = followSetsEvent
        .stream()
        .filter(AddressTag.class::isInstance)
        .map(AddressTag.class::cast)
        .toList();

    assert eventTags.size() == addressTags.size();

    return IntStream.range(0, eventTags.size())
        .mapToObj(i -> new EventTagAddressTagPair(
            eventTags.get(i),
            addressTags.get(i)))
        .toList();
  }

  private void deletePreviousFollowSetsEvent(GenericEventKind previousFollowSetsEvent) {
    cacheServiceIF.deleteEvent(
        new DeletionEvent(
            aImgIdentity,
            List.of(new EventTag(previousFollowSetsEvent.getId())), "aImg delete previous FOLLOW_SETS event"));
  }

  @Override
  public Kind getKind() {
    log.debug("{} getKind of Kind.FOLLOW_SETS 30_000", getClass().getSimpleName());
    return Kind.FOLLOW_SETS; // 30_000
  }
}
