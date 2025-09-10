package com.prosilion.afterimage.service;

import com.prosilion.afterimage.event.BadgeAwardDownvoteEvent;
import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.TextNoteEvent;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.EventKindServiceIF;
import com.prosilion.superconductor.base.service.event.service.EventKindTypeServiceIF;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import com.prosilion.superconductor.lib.redis.dto.GenericDocumentKindDto;
import com.prosilion.superconductor.lib.redis.dto.GenericDocumentKindTypeDto;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EmbeddedRedisStandalone
@ActiveProfiles("test")
class EventKindTypeServiceIT {
  private static final Logger log = LoggerFactory.getLogger(EventKindTypeServiceIT.class);
  private final EventKindServiceIF eventKindServiceIF;
  private final EventKindTypeServiceIF eventKindTypeService;
  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
  private final BadgeDefinitionEvent downvoteBadgeDefinitionEvent;

  private final RedisCacheServiceIF cacheIF;

  @Autowired
  public EventKindTypeServiceIT(
      EventKindServiceIF eventKindServiceIF,
      EventKindTypeServiceIF eventKindTypeService,
      BadgeDefinitionEvent upvoteBadgeDefinitionEvent,
      BadgeDefinitionEvent downvoteBadgeDefinitionEvent,
      RedisCacheServiceIF cacheIF) {
    this.eventKindServiceIF = eventKindServiceIF;
    this.eventKindTypeService = eventKindTypeService;
    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;
    this.downvoteBadgeDefinitionEvent = downvoteBadgeDefinitionEvent;
    this.cacheIF = cacheIF;

    log.info("EventKindTypeServiceIT initialized, EventKindServiceIF services: {}", this.eventKindServiceIF.getClass().getName());
    log.info("EventKindTypeServiceIT initialized, EventKindTypeServiceIF services: {}", this.eventKindTypeService.getClass().getName());
  }

  @Test
  void testUpvoteEvent() throws NostrException, NoSuchAlgorithmException {
    Identity voterIdentity = Identity.generateRandomIdentity();
    PublicKey upvotedUser = Identity.generateRandomIdentity().getPublicKey();

    BadgeAwardUpvoteEvent event1 = new BadgeAwardUpvoteEvent(
        voterIdentity,
        upvotedUser,
        upvoteBadgeDefinitionEvent);

    GenericDocumentKindTypeDto genericEventKindTypeDto = new GenericDocumentKindTypeDto(event1, SuperconductorKindType.UPVOTE);

    GenericEventKindTypeIF event = genericEventKindTypeDto.convertBaseEventToGenericEventKindTypeIF();

    eventKindTypeService.processIncomingEvent(event);

    List<? extends EventIF> eventsByKind = cacheIF.getByKind(upvoteBadgeDefinitionEvent.getKind());
    eventsByKind.forEach(System.out::println);
  }

  @Test
  void testDownvoteEvent() throws NostrException, NoSuchAlgorithmException {
    Identity identity = Identity.generateRandomIdentity();
    PublicKey downvotedUser = Identity.generateRandomIdentity().getPublicKey();

    BadgeAwardDownvoteEvent downvoteEvent = new BadgeAwardDownvoteEvent(identity, downvotedUser, downvoteBadgeDefinitionEvent);
    GenericEventKindTypeIF genericEventKindIF = new GenericDocumentKindTypeDto(downvoteEvent, SuperconductorKindType.DOWNVOTE).convertBaseEventToGenericEventKindTypeIF();
    eventKindTypeService.processIncomingEvent(genericEventKindIF);

    List<? extends EventIF> eventsByKind = cacheIF.getByKind(downvoteBadgeDefinitionEvent.getKind());
    eventsByKind.forEach(System.out::println);
  }

  @Test
  void testTextNoteEvent() throws NostrException, NoSuchAlgorithmException {
    Identity identity = Identity.generateRandomIdentity();

    TextNoteEvent textNoteEvent = new TextNoteEvent(identity, "TEXT note event text content");
    eventKindServiceIF.processIncomingEvent(new GenericDocumentKindDto(textNoteEvent).convertBaseEventToEventIF());

    List<? extends EventIF> eventsByKind = cacheIF.getByKind(textNoteEvent.getKind());

    assertEquals(1, eventsByKind.size());
    EventIF first = eventsByKind.getFirst();
    assertEquals(first.getKind(), textNoteEvent.getKind());
  }
}
