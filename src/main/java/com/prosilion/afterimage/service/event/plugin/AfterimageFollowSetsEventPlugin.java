package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.calculator.DynamicReputationCalculator;
import com.prosilion.afterimage.config.AfterimageBaseConfig;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.DeletionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FollowSetsEvent.EventTagAddressTagPair;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKind;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.type.PublishingEventKindPlugin;
import com.prosilion.superconductor.base.service.request.NotifierService;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageFollowSetsEventPlugin extends PublishingEventKindPlugin { // kind 30_000
  private final EventKindPluginIF reputationEventPlugin;
  private final RedisCacheServiceIF redisCacheServiceIF;
  private final Identity aImgIdentity;

  public AfterimageFollowSetsEventPlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity,
      @NonNull EventKindPluginIF reputationEventPlugin) {
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

    IdentifierTag identifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, incomingFollowSetsEvent)
        .stream()
        .findFirst().orElseThrow();

    Optional<GenericEventKind> existingFollowSetsEvent = getExistingFollowSetsEvent(voteReceiverPubkey, identifierTag);
//    TODO: ifPresent likely superfluous if delete mechanism already handles optional
    existingFollowSetsEvent.ifPresent(this::deletePreviousFollowSetsEvent);

    List<FollowSetsEvent.EventTagAddressTagPair> incomingPairs = getEventTagAddressTagPairs(incomingFollowSetsEvent.getTags());
    List<FollowSetsEvent.EventTagAddressTagPair> existingPairs = getEventTagAddressTagPairs(
        existingFollowSetsEvent.map(GenericEventKind::getTags).orElse(List.of()));

    List<FollowSetsEvent.EventTagAddressTagPair> nonMatches = incomingPairs.stream()
        .filter(incomingEventTagAddressTagPair ->
            !existingPairs.contains(incomingEventTagAddressTagPair)).toList();

    super.processIncomingEvent(
        createFollowSetsEvent(
            voteReceiverPubkey,
            Stream.concat(existingPairs.stream(), nonMatches.stream()).toList()));

    reputationEventPlugin.processIncomingEvent(
        createFollowSetsEvent(
            voteReceiverPubkey,
            nonMatches));
  }

  private Optional<GenericEventKind> getExistingFollowSetsEvent(
      PublicKey badgeReceiverPubkey,
      IdentifierTag uuid) {
    return redisCacheServiceIF
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
      @NonNull List<FollowSetsEvent.EventTagAddressTagPair> eventTagAddressTagPairs) {
    FollowSetsEvent followSetsEvent = new FollowSetsEvent(
        aImgIdentity,
        voteReceiverPubkey,
        new IdentifierTag(
            AfterimageBaseConfig.UNIT_REPUTATION),
        eventTagAddressTagPairs,
        DynamicReputationCalculator.class.getSimpleName());

    GenericEventRecord genericEventRecord = followSetsEvent.getGenericEventRecord();

    return genericEventRecord;
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

    return IntStream.range(0, eventTags.size())
        .mapToObj(i -> new EventTagAddressTagPair(
            eventTags.get(i),
            addressTags.get(i)))
        .toList();
  }

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
