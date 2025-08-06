package com.prosilion.afterimage.plugin;

import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.service.event.plugin.ReputationPublishingEventKindTypePlugin;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindType;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import com.prosilion.superconductor.lib.redis.document.EventDocumentIF;
import com.prosilion.superconductor.lib.redis.dto.GenericDocumentKindTypeDto;
import com.prosilion.superconductor.lib.redis.service.RedisCacheService;
import io.github.tobi.laa.spring.boot.embedded.redis.RedisFlushAll;
import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

//@Slf4j
@EmbeddedRedisStandalone
@RedisFlushAll(mode = RedisFlushAll.Mode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
//@DataRedisTest
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class ReputationPublishingEventKindTypePluginIT {

  private final ReputationPublishingEventKindTypePlugin repPlugin;
  private final RedisCacheService cacheServiceIF;
  private final List<GenericEventKindTypeIF> upvotesList = new ArrayList<>();

  private final Identity authorIdentity = Identity.generateRandomIdentity();
  private final PublicKey upvotedUser = Identity.generateRandomIdentity().getPublicKey();

  private final Integer votesCount;

  @Autowired
  public ReputationPublishingEventKindTypePluginIT(
      @NonNull @Value("${votesCount}") Integer votesCount,
      @NonNull RedisCacheService cacheServiceIF,
      @NonNull ReputationPublishingEventKindTypePlugin reputationPublishingEventKindTypePlugin,
      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent) throws NoSuchAlgorithmException {
    this.votesCount = votesCount;
    this.cacheServiceIF = cacheServiceIF;
    this.repPlugin = reputationPublishingEventKindTypePlugin;

    for (int i = 0; i < votesCount; i++) {
      upvotesList.add(createUpvoteDto(upvoteBadgeDefinitionEvent));
    }
    System.out.println("!!!!!!!!!!");
    System.out.println("!!!!!!!!!!");
    System.out.println("nvotesCount: " + votesCount);
    System.out.println("nvotesCount: " + votesCount);
    System.out.println("!!!!!!!!!!");
    System.out.println("!!!!!!!!!!");
  }

  private GenericEventKindTypeIF createUpvoteDto(BadgeDefinitionEvent upvoteBadgeDefinitionEvent) throws NoSuchAlgorithmException {
    return new GenericDocumentKindTypeDto(
        new BadgeAwardUpvoteEvent(
            authorIdentity,
            upvotedUser,
            upvoteBadgeDefinitionEvent),
        SuperconductorKindType.UPVOTE).convertBaseEventToGenericEventKindTypeIF();
  }

  @Test
  void testProcessIncomingEvent() {
//    printLogs(upvotesList);
    List<EventDocumentIF> all = cacheServiceIF.getAll();
    int allBeforeDeletions = all.size();
//    log.info("allBeforeDeletions size: {}", allBeforeDeletions);
    assertEquals(1, allBeforeDeletions);

    assertEquals(0, cacheServiceIF.getByKind(Kind.BADGE_AWARD_EVENT).size());

    assertEquals(1, cacheServiceIF.getByKind(Kind.BADGE_DEFINITION_EVENT).size());

    assertEquals(0, cacheServiceIF.getAllDeletionEventEntities().size());

    upvotesList.forEach(repPlugin::processIncomingEvent);

//    log.info("allAfterDeletions size: {}", cacheServiceIF.getAll().size());
//    TODO: below particularly troubling
//    assertEquals(allBeforeDeletions + 1, cacheServiceIF.getAll().size());

//    log.info("allDeletionEventEntities size: {}", cacheServiceIF.getAllDeletionEventEntities().size());
    assertEquals((votesCount - 1), cacheServiceIF.getAllDeletionEventEntities().size());

//    log.info("allPubkeyReputationEventsAfterDeltions size: {}", repPlugin.getAllPubkeyReputationEvents(upvotedUser).size());
    List<GenericEventKindType> reputationEvents = repPlugin.getAllPubkeyReputationEvents(upvotedUser);
    assertEquals(1, reputationEvents.size());
    assertEquals(votesCount.toString(), reputationEvents.getFirst().getContent()); 
    System.out.println("**********");
    System.out.println("**********");
    System.out.println("nvotesCount: " + votesCount);
    System.out.println("nvotesCount: " + votesCount);
    System.out.println("**********");
    System.out.println("**********");
  }

//  private void printLogs(List<? extends EventIF> list) {
//    log.info("for votesCount [{}]...", votesCount);
//    log.info("...reputations: ");
//    list.forEach(upvote -> {
//      log.info("-----");
//      log.info("event id:  ");
//      log.info("  {}", upvote.getId());
//      log.info("vote author pubkey:  ");
//      log.info("  {}", upvote.getPublicKey().toString());
//      log.info("kind:  ");
//      log.info("  {} : {}", upvote.getKind().getName().toUpperCase(), upvote.getKind().toString());
////      log.info("kind type:  ");
////      log.info("  {} : {}", upvote.getKindType(), upvote.getKindType().getKindDefinition().getValue());
//      log.info("tags:  ");
//      upvote.getTags().forEach(tag -> log.info("  {}", tag.toString()));
//      log.info("content:  ");
//      log.info("  {}", upvote.getContent());
//      log.info("\n");
//    });
//    log.info("end reputations printLog()\n");
//  }
}
