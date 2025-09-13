package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.service.AfterimageReputationCalculator;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKind;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindPlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import com.prosilion.superconductor.lib.redis.document.EventDocumentIF;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageFollowSetsEventPlugin extends PublishingEventKindPlugin { // kind 30_000
  private final EventKindTypePluginIF reputationEventPlugin;
  private final RedisCacheServiceIF redisCacheServiceIF;
  private final Identity aImgIdentity;

  public AfterimageFollowSetsEventPlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull EventKindTypePluginIF reputationEventPlugin) {
    super(notifierService, eventKindPlugin);
    this.reputationEventPlugin = reputationEventPlugin;
    this.redisCacheServiceIF = redisCacheServiceIF;
    this.aImgIdentity = aImgIdentity;
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF incomingFollowSetsEvent) {
    log.debug("{}} processing incoming Kind.FOLLOW_SETS 30_000 : [{}]", getClass().getSimpleName(), incomingFollowSetsEvent);
    PublicKey voteReceiverPubkey = Filterable.getTypeSpecificTags(PubKeyTag.class, incomingFollowSetsEvent)
        .stream()
        .map(PubKeyTag::getPublicKey)
        .findFirst().orElseThrow();
    
/*{
  "kind": 30000,
  "pubkey": "<AIMG_RELAY_PUBKEY>",
  "tags": [
    ["d", "<AimgRepCalculationClass>"],
    ["p", "VOTE_RECIP_1_PUBKEY", "ws://sc.url:port"],

    ["e", "VOTE_EVENT_ID_1", "ws://sc.url:port"],
    ["a", "30009:SC_PUBKEY:upvote"],

    ["e", "VOTE_EVENT_ID_2", "ws://sc.url:port"],
    ["a", "30009:SC_PUBKEY:downvote"],

  "content": current REP score 
}*/

    Optional<GenericEventKind> existingFollowSetsEvent = getExistingFollowSetsEvent(voteReceiverPubkey);
    existingFollowSetsEvent.ifPresent(this::deletePreviousFollowSetsEvent);
//    if (existingFollowSetsEvents.isEmpty()) {
//      reputationEventPlugin.processIncomingEvent(incomingFollowSetsEvent);
//      return;
//    }

    List<FollowSetsEvent.EventTagAddressTagPair> incomingPairs = getEventTagAddressTagPairs(incomingFollowSetsEvent.getTags());
    List<FollowSetsEvent.EventTagAddressTagPair> existingPairs = getEventTagAddressTagPairs(
        existingFollowSetsEvent.map(GenericEventKind::getTags).orElse(List.of()));

    List<FollowSetsEvent.EventTagAddressTagPair> nonMatches = incomingPairs.stream()
        .filter(incomingEventTagAddressTagPair ->
            !existingPairs.contains(incomingEventTagAddressTagPair)).toList();

    EventIF updatedFollowSetsEvent = createFollowSetsEvent(
        voteReceiverPubkey,
        Stream.concat(existingPairs.stream(), nonMatches.stream()).toList());
    super.processIncomingEvent(updatedFollowSetsEvent);

    EventIF xorFollowSetsEvent = createFollowSetsEvent(
        voteReceiverPubkey,
        nonMatches);
    reputationEventPlugin.processIncomingEvent(xorFollowSetsEvent);
  }

  public Optional<GenericEventKind> getExistingFollowSetsEvent(PublicKey badgeReceiverPubkey) {
    List<EventDocumentIF> eventsByKindAndPubKeyTag = redisCacheServiceIF
        .getEventsByKindAndPubKeyTag(Kind.FOLLOW_SETS, badgeReceiverPubkey);

    Optional<EventDocumentIF> max = eventsByKindAndPubKeyTag
        .stream()
        .max(Comparator.comparing(EventIF::getCreatedAt));

    return max
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

  @SneakyThrows
  public EventIF createFollowSetsEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull List<FollowSetsEvent.EventTagAddressTagPair> eventTagAddressTagPairs) {
    return new FollowSetsEvent(
        aImgIdentity,
        voteReceiverPubkey,
        eventTagAddressTagPairs,
        AfterimageReputationCalculator.class.getSimpleName()).getGenericEventRecord();
  }

  public List<FollowSetsEvent.EventTagAddressTagPair> getEventTagAddressTagPairs(List<BaseTag> followSetsEvent) {
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

    List<FollowSetsEvent.EventTagAddressTagPair> pairs = IntStream.range(0, eventTags.size())
        .mapToObj(i -> new FollowSetsEvent.EventTagAddressTagPair(
            eventTags.get(i),
            addressTags.get(i)))
        .toList();
    return pairs;
  }

  @SneakyThrows
  private void deletePreviousFollowSetsEvent(GenericEventKind previousFollowSetsEvent) {
    redisCacheServiceIF.deleteEvent(
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
