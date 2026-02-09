//package com.prosilion.afterimage.plugin;
//
//import com.prosilion.afterimage.event.internal.BadgeAwardUpvoteEvent;
//import com.prosilion.afterimage.service.event.plugin.ReputationEventPlugin;
//import com.prosilion.nostr.enums.Kind;
//import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
//import com.prosilion.nostr.event.EventIF;
//import com.prosilion.nostr.tag.AddressTag;
//import com.prosilion.nostr.user.Identity;
//import com.prosilion.nostr.user.PublicKey;
//import com.prosilion.superconductor.base.service.event.plugin.kind.ParameterizedEventKindPluginIF;
//import com.prosilion.superconductor.lib.redis.entity.EventNosqlEntityIF;
//import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
//import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.lang.NonNull;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.ActiveProfiles;
//
//import static java.util.concurrent.TimeUnit.SECONDS;
//import static org.awaitility.Awaitility.await;
//import static org.junit.jupiter.api.Assertions.assertAll;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//
//@EmbeddedRedisStandalone
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
//@ActiveProfiles("test")
//public class ReputationEventPluginIT {
//
//  private final BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent;
//
//  private final ReputationEventPlugin repPlugin;
//  private final RedisCacheServiceIF cacheServiceIF;
//  private final List<EventIF> upvotesList = new ArrayList<>();
//
//  private final Identity authorIdentity = Identity.generateRandomIdentity();
//  private final PublicKey upvotedUser = Identity.generateRandomIdentity().getPublicKey();
//
//  private final Integer votesCount;
//
//  private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
//
//  @Autowired
//  public ReputationEventPluginIT(
//      @NonNull @Value("${votesCount}") Integer votesCount,
//      @NonNull RedisCacheServiceIF cacheServiceIF,
//      @NonNull EventKindPluginIF reputationEventPlugin,
//      @NonNull BadgeDefinitionAwardEvent badgeAwardDefinitionEvent) {
//    this.votesCount = votesCount;
//    this.cacheServiceIF = cacheServiceIF;
//    this.repPlugin = (ReputationEventPlugin) reputationEventPlugin;
//    this.badgeDefinitionUpvoteEvent = badgeAwardDefinitionEvent;
//
//    for (int i = 0; i < votesCount; i++) {
//      upvotesList.add(createUpvoteDto(badgeAwardDefinitionEvent));
//    }
//  }
//
//  @Test
//  void testRelaySetsOrder() {
//
//  }
//
//  @Test
//  void testProcessIncomingEvent() {
//    List<EventNosqlEntityIF> all = cacheServiceIF.getAll();
//
//    int sizeOfGetAllBeforeDeletions = all.size();
//    assertEquals(1, sizeOfGetAllBeforeDeletions);
//
//    assertEquals(0, cacheServiceIF.getByKind(Kind.BADGE_AWARD_EVENT).size());
//    assertEquals(1, cacheServiceIF.getByKind(Kind.BADGE_DEFINITION_EVENT).size());
//    assertEquals(0, cacheServiceIF.getAllDeletionEvents().size());
//
//    await().until(() ->
//        processIncomingEventExecutor().isDone());
//
//    assertEquals((votesCount - 1), cacheServiceIF.getAllDeletionEvents().size());
//
//    AddressTag addressTag = new AddressTag(Kind.BADGE_DEFINITION_EVENT, badgeDefinitionUpvoteEvent.getPublicKey(), badgeDefinitionUpvoteEvent.getIdentifierTag());
//
//    EventIF reputationEvents = repPlugin.getExistingReputationEvent(upvotedUser, addressTag).orElseThrow();
//    assertEquals(votesCount.toString(), reputationEvents.getContent());
//
//    assertEquals(votesCount, cacheServiceIF.getByKind(Kind.BADGE_AWARD_EVENT).size());
//    assertEquals(1, cacheServiceIF.getByKind(Kind.BADGE_DEFINITION_EVENT).size());
//
////    process another vote- and subsequently- another updated (single) reputation
//    repPlugin.processIncomingEvent(createUpvoteDto(badgeDefinitionUpvoteEvent));
//    assertEquals(votesCount, cacheServiceIF.getAllDeletionEvents().size());
//
//    Optional<EventIF> anotherReputationEvent = repPlugin.getExistingReputationEvent(upvotedUser, addressTag);
//    assertEquals(
//        Integer.valueOf(votesCount + 1).toString(),
//        anotherReputationEvent.orElseThrow().getContent());
//
//    assertEquals((votesCount + 1), cacheServiceIF.getByKind(Kind.BADGE_AWARD_EVENT).size());
//    assertEquals(1, cacheServiceIF.getAll().stream().map(EventNosqlEntityIF::getEventId)
//        .filter(id -> anotherReputationEvent.orElseThrow().getId().equals(id))
//        .toList().size());
//  }
//
//  private CompletableFuture<Void> processIncomingEventExecutor() throws ExecutionException, InterruptedException {
//    CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
//            upvotesList.forEach(eventKindTypeIF ->
//                assertAll(() -> repPlugin.processIncomingEvent(eventKindTypeIF)))
//        , executorService);
//
//    future.get();
//
//    await()
//        .timeout(5, SECONDS)
//        .until(future::isDone);
//
//    assertFalse(future.isCompletedExceptionally());
//    return future;
//  }
//
//  private EventIF createUpvoteDto(BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent) {
//    return new BadgeAwardUpvoteEvent(
//        authorIdentity,
//        upvotedUser,
//        badgeDefinitionUpvoteEvent);
//  }
//}
