package com.prosilion.afterimage.plugin;

import com.prosilion.afterimage.calculator.UnitReputationCalculator;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.service.event.plugin.AfterimageFollowSetsEventPlugin;
import com.prosilion.afterimage.service.event.plugin.ReputationEventPlugin;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FollowSetsEvent.EventTagAddressTagPair;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKind;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.lang.NonNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EmbeddedRedisStandalone
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ActiveProfiles("test")
public class FollowSetsEventPairOrderIT {
  private final AfterimageFollowSetsEventPlugin afterimageFollowSetsEventPlugin;
  private final ReputationEventPlugin reputationEventPlugin;

  private final Identity authorIdentity = Identity.generateRandomIdentity();
  private final PublicKey upvotedUser = Identity.generateRandomIdentity().getPublicKey();

  @Autowired
  public FollowSetsEventPairOrderIT(
      @NonNull EventKindPluginIF afterimageFollowSetsEventPlugin,
      @NonNull EventKindTypePluginIF reputationEventPlugin) {
    this.afterimageFollowSetsEventPlugin = (AfterimageFollowSetsEventPlugin) afterimageFollowSetsEventPlugin;
    this.reputationEventPlugin = (ReputationEventPlugin) reputationEventPlugin;
  }

  @Test
  void testEventTagAddressTagPairsOrderAfterSaved() throws NoSuchAlgorithmException {
    List<EventTagAddressTagPair> pairs_1 = createPairs(1);
    FollowSetsEvent followSetsEvent_1 = createFollowSetsEvent(pairs_1);

    afterimageFollowSetsEventPlugin.processIncomingEvent(followSetsEvent_1);

    Optional<GenericEventKind> savedFollowSetsEvent_1 = afterimageFollowSetsEventPlugin.getExistingFollowSetsEvent(upvotedUser);
    Optional<EventIF> savedReputationEvent_1 = reputationEventPlugin.getExistingReputationEvent(upvotedUser);
    assertEquals("1", savedReputationEvent_1.orElseThrow().getContent());

    List<EventTagAddressTagPair> pairs_2 = createPairs(1);
    FollowSetsEvent followSetsEvent_2 = createFollowSetsEvent(Stream.concat(pairs_1.stream(), pairs_2.stream()).toList());

    afterimageFollowSetsEventPlugin.processIncomingEvent(followSetsEvent_2);

    Optional<GenericEventKind> savedFollowSetsEvent_2 = afterimageFollowSetsEventPlugin.getExistingFollowSetsEvent(upvotedUser);
    Optional<EventIF> savedReputationEvent_2 = reputationEventPlugin.getExistingReputationEvent(upvotedUser);
    assertEquals("2", savedReputationEvent_2.orElseThrow().getContent());

    List<EventTagAddressTagPair> pairs_3 = createPairs(1);
    List<EventTagAddressTagPair> pairs_4 = createPairs(1);

    List<EventTagAddressTagPair> group5 = Stream.concat(Stream.concat(Stream.concat(pairs_1.stream(), pairs_2.stream()), pairs_3.stream()), pairs_4.stream()).toList();

    FollowSetsEvent followSetsEvent_5 = createFollowSetsEvent(group5);
    afterimageFollowSetsEventPlugin.processIncomingEvent(followSetsEvent_5);

    Optional<GenericEventKind> savedFollowSetsEvent_5 = afterimageFollowSetsEventPlugin.getExistingFollowSetsEvent(upvotedUser);
    Optional<EventIF> savedReputationEvent_5 = reputationEventPlugin.getExistingReputationEvent(upvotedUser);
    assertEquals("4", savedReputationEvent_5.orElseThrow().getContent());

    List<EventTagAddressTagPair> pairs_6 = createPairs(1);
    List<EventTagAddressTagPair> pairs_7 = createPairs(1);

    List<EventTagAddressTagPair> group8 = Stream.concat(Stream.concat(group5.stream(), pairs_6.stream()), pairs_7.stream()).toList();

    FollowSetsEvent followSetsEvent_8 = createFollowSetsEvent(group8);
    afterimageFollowSetsEventPlugin.processIncomingEvent(followSetsEvent_8);

    Optional<GenericEventKind> savedFollowSetsEvent_8 = afterimageFollowSetsEventPlugin.getExistingFollowSetsEvent(upvotedUser);
    Optional<EventIF> savedReputationEvent_8 = reputationEventPlugin.getExistingReputationEvent(upvotedUser);
    assertEquals("6", savedReputationEvent_8.orElseThrow().getContent());

    List<EventTagAddressTagPair> pairs_9 = createPairs(2);

    List<EventTagAddressTagPair> group10 = Stream.concat(group8.stream(), pairs_9.stream()).toList();

    FollowSetsEvent followSetsEvent_10 = createFollowSetsEvent(group10);
    afterimageFollowSetsEventPlugin.processIncomingEvent(followSetsEvent_10);

    Optional<GenericEventKind> savedFollowSetsEvent_10 = afterimageFollowSetsEventPlugin.getExistingFollowSetsEvent(upvotedUser);
    Optional<EventIF> savedReputationEvent_10 = reputationEventPlugin.getExistingReputationEvent(upvotedUser);
    assertEquals("6", savedReputationEvent_10.orElseThrow().getContent());

    List<EventTagAddressTagPair> pairs_11 = createPairs(10);

    List<EventTagAddressTagPair> group12 = Stream.concat(group10.stream(), pairs_11.stream()).toList();

    FollowSetsEvent followSetsEvent_12 = createFollowSetsEvent(group12);
    afterimageFollowSetsEventPlugin.processIncomingEvent(followSetsEvent_12);

    Optional<GenericEventKind> savedFollowSetsEvent_12 = afterimageFollowSetsEventPlugin.getExistingFollowSetsEvent(upvotedUser);
    Optional<EventIF> savedReputationEvent_12 = reputationEventPlugin.getExistingReputationEvent(upvotedUser);
    assertEquals("6", savedReputationEvent_12.orElseThrow().getContent());
    assertEquals("6", savedReputationEvent_12.orElseThrow().getContent());
  }

  private @NotNull FollowSetsEvent createFollowSetsEvent(List<EventTagAddressTagPair> pairs) throws NoSuchAlgorithmException {
    return new FollowSetsEvent(
        authorIdentity,
        upvotedUser,
        new IdentifierTag(
            UnitReputationCalculator.class.getCanonicalName()),
        pairs,
        UnitReputationCalculator.class.getName());
  }

  private List<EventTagAddressTagPair> createPairs(int size) {
    int startIndex = 0;
    List<EventTag> eventTags = IntStream.range(startIndex, size)
        .mapToObj(i ->
            new EventTag(Factory.generateRandomHex64String(), "aImgUrl"))
        .toList();

    List<AddressTag> addressTags = IntStream.range(startIndex, size)
        .mapToObj(i -> new AddressTag(
            Kind.BADGE_AWARD_EVENT,
            authorIdentity.getPublicKey(),
            new IdentifierTag(getKindType(i).getName())))
        .toList();

    return IntStream.range(startIndex, size)
        .mapToObj(i -> new EventTagAddressTagPair(
            eventTags.get(i),
            addressTags.get(i)))
        .toList();
  }

  private SuperconductorKindType getKindType(int i) {
    if (i % 2 == 0)
      return SuperconductorKindType.UNIT_UPVOTE;
    return SuperconductorKindType.UNIT_DOWNVOTE;
  }

  @Test
  void testEventTagAddressTagPairsOrder() throws NoSuchAlgorithmException {
    EventTag firstEventTag = new EventTag(
        Factory.generateRandomHex64String(),
        "aImgUrl");
    AddressTag firstAddressTag = new AddressTag(
        Kind.BADGE_AWARD_EVENT,
        authorIdentity.getPublicKey(),
        new IdentifierTag(AfterimageKindType.REPUTATION.getName()));

    int startIndex = 0;
    int size = 8;

    List<EventTag> eventTags = IntStream.range(startIndex, size)
        .mapToObj(i ->
            new EventTag(Factory.generateRandomHex64String(), "aImgUrl"))
        .toList();

    List<AddressTag> addressTags = IntStream.range(startIndex, size)
        .mapToObj(i -> new AddressTag(
            Kind.valueOf(i),
            authorIdentity.getPublicKey(),
            new IdentifierTag(String.valueOf(i))))
        .toList();

    List<EventTagAddressTagPair> expectedPairsOrder = IntStream.range(startIndex, size)
        .mapToObj(i -> new EventTagAddressTagPair(
            eventTags.get(i),
            addressTags.get(i)))
        .toList();

    FollowSetsEvent followSetsEvent = new FollowSetsEvent(
        authorIdentity,
        upvotedUser,
        new IdentifierTag(
            UnitReputationCalculator.class.getCanonicalName()),
        expectedPairsOrder,
        UnitReputationCalculator.class.getName());

    List<EventTagAddressTagPair> actualPairsOrder = afterimageFollowSetsEventPlugin.getEventTagAddressTagPairs(followSetsEvent.getTags());

    for (int i = startIndex; i < actualPairsOrder.size(); i++) {
      EventTagAddressTagPair expectedEventTagAddressTagPair = expectedPairsOrder.get(i);
      EventTagAddressTagPair actualEventTagAddressTagPair = actualPairsOrder.get(i);
      assertEquals(expectedEventTagAddressTagPair.getTags(), actualEventTagAddressTagPair.getTags());
      assertEquals(expectedEventTagAddressTagPair.eventTag(), actualEventTagAddressTagPair.eventTag());
      assertEquals(expectedEventTagAddressTagPair.addressTag(), actualEventTagAddressTagPair.addressTag());
    }
  }
}
