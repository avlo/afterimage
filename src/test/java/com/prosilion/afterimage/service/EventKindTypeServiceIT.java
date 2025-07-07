package com.prosilion.afterimage.service;

import com.prosilion.afterimage.event.BadgeAwardDownvoteEvent;
import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.GenericEventKindIF;
import com.prosilion.nostr.event.GenericEventKindTypeIF;
import com.prosilion.nostr.event.TextNoteEvent;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.dto.GenericEventKindDto;
import com.prosilion.superconductor.dto.GenericEventKindTypeDto;
import com.prosilion.superconductor.service.event.service.EventKindServiceIF;
import com.prosilion.superconductor.service.event.service.EventKindTypeServiceIF;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.SuperconductorKindType;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EventKindTypeServiceIT {
  private static final Logger log = LoggerFactory.getLogger(EventKindTypeServiceIT.class);
  private final EventKindServiceIF eventKindService;
  private final EventKindTypeServiceIF eventKindTypeService;
  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
  private final BadgeDefinitionEvent downvoteBadgeDefinitionEvent;

  private final EventEntityService eventEntityService;

  @Autowired
  public EventKindTypeServiceIT(
      EventKindServiceIF eventKindService,
      EventKindTypeServiceIF eventKindTypeService,
      BadgeDefinitionEvent upvoteBadgeDefinitionEvent,
      BadgeDefinitionEvent downvoteBadgeDefinitionEvent,
      EventEntityService eventEntityService) {
    this.eventKindService = eventKindService;
    this.eventKindTypeService = eventKindTypeService;
    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;
    this.downvoteBadgeDefinitionEvent = downvoteBadgeDefinitionEvent;
    this.eventEntityService = eventEntityService;

    log.info("EventKindTypeServiceIT initialized, EventKindServiceIF services: {}", this.eventKindService.getClass().getName());
    log.info("EventKindTypeServiceIT initialized, EventKindTypeServiceIF services: {}", this.eventKindTypeService.getClass().getName());
  }

  @Test
  void testUpvoteEvent() throws NostrException, NoSuchAlgorithmException {
    Identity voterIdentity = Identity.generateRandomIdentity();
    PublicKey upvotedUser = Identity.generateRandomIdentity().getPublicKey();

    eventKindTypeService.processIncomingEvent(
        new GenericEventKindTypeDto(
            new BadgeAwardUpvoteEvent(
                voterIdentity,
                upvotedUser,
                upvoteBadgeDefinitionEvent),
            SuperconductorKindType.UPVOTE).convertBaseEventToGenericEventKindTypeIF());

    List<GenericEventKindIF> eventsByKind = eventEntityService.getEventsByKind(upvoteBadgeDefinitionEvent.getKind());
    eventsByKind.forEach(System.out::println);
  }

  @Test
  void testDownvoteEvent() throws NostrException, NoSuchAlgorithmException {
    Identity identity = Identity.generateRandomIdentity();
    PublicKey downvotedUser = Identity.generateRandomIdentity().getPublicKey();

    BadgeAwardDownvoteEvent downvoteEvent = new BadgeAwardDownvoteEvent(identity, downvotedUser, downvoteBadgeDefinitionEvent);
    GenericEventKindTypeIF genericEventKindIF = new GenericEventKindTypeDto(downvoteEvent, SuperconductorKindType.DOWNVOTE).convertBaseEventToGenericEventKindTypeIF();
    eventKindTypeService.processIncomingEvent(genericEventKindIF);

    List<GenericEventKindIF> eventsByKind = eventEntityService.getEventsByKind(downvoteBadgeDefinitionEvent.getKind());
    eventsByKind.forEach(System.out::println);
  }

  @Test
  void testTextNoteEvent() throws NostrException, NoSuchAlgorithmException {
    Identity identity = Identity.generateRandomIdentity();

    TextNoteEvent textNoteEvent = new TextNoteEvent(identity, "TEXT note event text content");
    eventKindService.processIncomingEvent(new GenericEventKindDto(textNoteEvent).convertBaseEventToGenericEventKindIF());

    List<GenericEventKindIF> eventsByKind = eventEntityService.getEventsByKind(textNoteEvent.getKind());
    eventsByKind.forEach(System.out::println);
  }
}
