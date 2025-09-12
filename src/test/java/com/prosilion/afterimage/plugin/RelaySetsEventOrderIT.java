package com.prosilion.afterimage.plugin;

import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.service.AfterimageReputationCalculator;
import com.prosilion.afterimage.service.event.plugin.ReputationEventPlugin;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FollowSetsEvent.EventTagAddressTagPair;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import com.prosilion.superconductor.lib.redis.dto.GenericDocumentKindTypeDto;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.lang.NonNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@EmbeddedRedisStandalone
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ActiveProfiles("test")
public class RelaySetsEventOrderIT {
  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
  private final ReputationEventPlugin reputationEventPlugin;
  private final RedisCacheServiceIF cacheServiceIF;
  private final List<GenericEventKindTypeIF> upvotesList = new ArrayList<>();

  private final Identity authorIdentity = Identity.generateRandomIdentity();
  private final PublicKey upvotedUser = Identity.generateRandomIdentity().getPublicKey();

  private final Integer votesCount;
  private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

  @Autowired
  public RelaySetsEventOrderIT(
      @NonNull @Value("${votesCount}") Integer votesCount,
      @NonNull RedisCacheServiceIF cacheServiceIF,
      @NonNull EventKindTypePluginIF reputationEventPlugin,
      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent) throws NoSuchAlgorithmException {
    this.votesCount = votesCount;
    this.cacheServiceIF = cacheServiceIF;
    this.reputationEventPlugin = (ReputationEventPlugin) reputationEventPlugin;
    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;

//    for (int i = 0; i < votesCount; i++) {
//      upvotesList.add(createUpvoteDto(upvoteBadgeDefinitionEvent));
//    }
  }

  @Test
  void testEventTagAddressTagPairsOrder() throws NoSuchAlgorithmException {
    int size = 8;

    List<EventTag> eventTags = IntStream.range(0, size)
        .mapToObj(i -> new EventTag(Factory.generateRandomHex64String(), "aImgUrl")).toList();

    List<AddressTag> addressTags = IntStream.range(0, size)
        .mapToObj(i -> new AddressTag(
            Kind.valueOf(i),
            authorIdentity.getPublicKey(),
            new IdentifierTag(String.valueOf(i)))).toList();

    List<EventTagAddressTagPair> expectedPairsOrder = IntStream.range(0, size)
        .mapToObj(i -> new EventTagAddressTagPair(
            eventTags.get(i),
            addressTags.get(i)))
        .toList();

    FollowSetsEvent followSetsEvent = new FollowSetsEvent(
        authorIdentity,
        upvotedUser,
        expectedPairsOrder,
        AfterimageReputationCalculator.class.getName());

    List<EventTagAddressTagPair> actualPairsOrder = reputationEventPlugin.getEventTagAddressTagPairs(followSetsEvent.getTags());

    for (int i = 0; i < actualPairsOrder.size(); i++) {
      EventTagAddressTagPair expectedEventTagAddressTagPair = expectedPairsOrder.get(i);
      EventTagAddressTagPair actualEventTagAddressTagPair = actualPairsOrder.get(i);
      assertEquals(expectedEventTagAddressTagPair.getTags(), actualEventTagAddressTagPair.getTags());
      assertEquals(expectedEventTagAddressTagPair.eventTag(), actualEventTagAddressTagPair.eventTag());
      assertEquals(expectedEventTagAddressTagPair.addressTag(), actualEventTagAddressTagPair.addressTag());
    }
  }

  private CompletableFuture<Void> processIncomingEventExecutor() throws ExecutionException, InterruptedException {
    CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
            upvotesList.forEach(eventKindTypeIF ->
                assertAll(() -> reputationEventPlugin.processIncomingEvent(eventKindTypeIF)))
        , executorService);

    future.get();

    await()
        .timeout(5, SECONDS)
        .until(future::isDone);

    assertFalse(future.isCompletedExceptionally());
    return future;
  }

  private GenericEventKindTypeIF createUpvoteDto(BadgeDefinitionEvent upvoteBadgeDefinitionEvent) throws NoSuchAlgorithmException {
    return new GenericDocumentKindTypeDto(
        new BadgeAwardUpvoteEvent(
            authorIdentity,
            upvotedUser,
            upvoteBadgeDefinitionEvent),
        SuperconductorKindType.UPVOTE).convertBaseEventToGenericEventKindTypeIF();
  }
}
