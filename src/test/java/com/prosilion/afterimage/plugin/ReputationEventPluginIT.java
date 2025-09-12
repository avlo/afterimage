package com.prosilion.afterimage.plugin;

import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.service.event.plugin.ReputationEventPlugin;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindType;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import com.prosilion.superconductor.lib.redis.document.EventDocumentIF;
import com.prosilion.superconductor.lib.redis.dto.GenericDocumentKindTypeDto;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public class ReputationEventPluginIT {

  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;

  private final ReputationEventPlugin repPlugin;
  private final RedisCacheServiceIF cacheServiceIF;
  private final List<GenericEventKindTypeIF> upvotesList = new ArrayList<>();

  private final Identity authorIdentity = Identity.generateRandomIdentity();
  private final PublicKey upvotedUser = Identity.generateRandomIdentity().getPublicKey();

  private final Integer votesCount;

  private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

  @Autowired
  public ReputationEventPluginIT(
      @NonNull @Value("${votesCount}") Integer votesCount,
      @NonNull RedisCacheServiceIF cacheServiceIF,
      @NonNull EventKindTypePluginIF reputationEventPlugin,
      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent) throws NoSuchAlgorithmException {
    this.votesCount = votesCount;
    this.cacheServiceIF = cacheServiceIF;
    this.repPlugin = (ReputationEventPlugin) reputationEventPlugin;
    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;

    for (int i = 0; i < votesCount; i++) {
      upvotesList.add(createUpvoteDto(upvoteBadgeDefinitionEvent));
    }
  }

  @Test
  void testProcessIncomingEvent() throws NoSuchAlgorithmException {
    List<EventDocumentIF> all = cacheServiceIF.getAll();
    int sizeOfGetAllBeforeDeletions = all.size();
    assertEquals(1, sizeOfGetAllBeforeDeletions);

    assertEquals(0, cacheServiceIF.getByKind(Kind.BADGE_AWARD_EVENT).size());
    assertEquals(1, cacheServiceIF.getByKind(Kind.BADGE_DEFINITION_EVENT).size());
    assertEquals(0, cacheServiceIF.getAllDeletionEvents().size());

    await().until(() ->
        processIncomingEventExecutor().isDone());

    assertEquals((votesCount - 1), cacheServiceIF.getAllDeletionEvents().size());

    GenericEventKindType reputationEvents = repPlugin.getPreviousReputationEvent(upvotedUser).orElseThrow();
    assertEquals(votesCount.toString(), reputationEvents.getContent());

    assertEquals(votesCount*2, cacheServiceIF.getByKind(Kind.BADGE_AWARD_EVENT).size());
    assertEquals(1, cacheServiceIF.getByKind(Kind.BADGE_DEFINITION_EVENT).size());

//    process another vote- and subsequently- another updated (single) reputation
    repPlugin.processIncomingEvent(createUpvoteDto(upvoteBadgeDefinitionEvent));
    assertEquals(votesCount, cacheServiceIF.getAllDeletionEvents().size());

    Optional<GenericEventKindType> anotherReputationEvent = repPlugin.getPreviousReputationEvent(upvotedUser);
    assertEquals(
        Integer.valueOf(votesCount + 1).toString(),
        anotherReputationEvent.orElseThrow().getContent());

    assertEquals((votesCount + 1)*2, cacheServiceIF.getByKind(Kind.BADGE_AWARD_EVENT).size());
    assertEquals(1, cacheServiceIF.getAll().stream().map(EventDocumentIF::getEventId)
        .filter(id -> anotherReputationEvent.orElseThrow().getId().equals(id))
        .toList().size());
  }

  private CompletableFuture<Void> processIncomingEventExecutor() throws ExecutionException, InterruptedException {
    CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
            upvotesList.forEach(eventKindTypeIF ->
                assertAll(() -> repPlugin.processIncomingEvent(eventKindTypeIF)))
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
