package com.prosilion.afterimage.service;

import com.prosilion.afterimage.event.internal.BadgeAwardDownvoteEvent;
import com.prosilion.afterimage.event.internal.BadgeAwardUpvoteEvent;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.TextNoteEvent;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.EventKindServiceIF;
import com.prosilion.superconductor.lib.redis.dto.GenericNosqlEntityKindDto;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class EventKindTypeServiceIT {
  private static final Logger log = LoggerFactory.getLogger(EventKindTypeServiceIT.class);
  private final EventKindServiceIF eventKindServiceIF;
  private final BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent;
  private final BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent;

  private final RedisCacheServiceIF cacheIF;

  @Autowired
  public EventKindTypeServiceIT(
      EventKindServiceIF eventKindServiceIF,
      BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent,
      BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent,
      RedisCacheServiceIF cacheIF) {
    this.eventKindServiceIF = eventKindServiceIF;
    this.badgeDefinitionUpvoteEvent = badgeDefinitionUpvoteEvent;
    this.badgeDefinitionDownvoteEvent = badgeDefinitionDownvoteEvent;
    this.cacheIF = cacheIF;

    log.info("EventKindTypeServiceIT initialized, EventKindServiceIF services: {}", this.eventKindServiceIF.getClass().getName());
  }

  @Test
  void testUpvoteEvent() throws NostrException {
    Identity voterIdentity = Identity.generateRandomIdentity();
    PublicKey upvotedUser = Identity.generateRandomIdentity().getPublicKey();

    BadgeAwardUpvoteEvent event1 = new BadgeAwardUpvoteEvent(
        voterIdentity,
        upvotedUser,
        badgeDefinitionUpvoteEvent);

    EventIF event = new GenericNosqlEntityKindDto(event1).convertBaseEventToEventIF();

    eventKindServiceIF.processIncomingEvent(event);

    List<? extends EventIF> eventsByKind = cacheIF.getByKind(badgeDefinitionUpvoteEvent.getKind());
    eventsByKind.forEach(System.out::println);
  }

  @Test
  void testDownvoteEvent() throws NostrException {
    Identity identity = Identity.generateRandomIdentity();
    PublicKey downvotedUser = Identity.generateRandomIdentity().getPublicKey();

    BadgeAwardDownvoteEvent downvoteEvent = new BadgeAwardDownvoteEvent(identity, downvotedUser, badgeDefinitionDownvoteEvent);
    EventIF genericEventKindIF = new GenericNosqlEntityKindDto(downvoteEvent).convertBaseEventToEventIF();
    eventKindServiceIF.processIncomingEvent(genericEventKindIF);

    List<? extends EventIF> eventsByKind = cacheIF.getByKind(badgeDefinitionDownvoteEvent.getKind());
    eventsByKind.forEach(System.out::println);
  }

  @Test
  void testTextNoteEvent() throws NostrException {
    Identity identity = Identity.generateRandomIdentity();

    TextNoteEvent textNoteEvent = new TextNoteEvent(identity, "TEXT note event text content");
    eventKindServiceIF.processIncomingEvent(new GenericNosqlEntityKindDto(textNoteEvent).convertBaseEventToEventIF());

    List<? extends EventIF> eventsByKind = cacheIF.getByKind(textNoteEvent.getKind());

    assertEquals(1, eventsByKind.size());
    EventIF first = eventsByKind.getFirst();
    assertEquals(first.getKind(), textNoteEvent.getKind());
  }
}
