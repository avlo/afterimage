package com.prosilion.afterimage.service;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.BadgeAwardDownvoteEvent;
import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.GenericEventKindTypeIF;
import com.prosilion.nostr.event.TextNoteEvent;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.dto.GenericEventKindDto;
import com.prosilion.superconductor.dto.GenericEventKindTypeDto;
import com.prosilion.superconductor.service.event.service.EventKindServiceIF;
import com.prosilion.superconductor.service.event.service.EventKindTypeServiceIF;
import java.security.NoSuchAlgorithmException;
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
  private final EventKindServiceIF<Kind> eventKindService;
  private final EventKindTypeServiceIF<KindTypeIF> eventKindTypeService;

  @Autowired
  public EventKindTypeServiceIT(
      EventKindServiceIF<Kind> eventKindService,
      EventKindTypeServiceIF<KindTypeIF> eventKindTypeService) {
    this.eventKindService = eventKindService;
    this.eventKindTypeService = eventKindTypeService;
    log.info("EventKindTypeServiceIT initialized, EventKindServiceIF services: {}", this.eventKindService.getClass().getName());
    log.info("EventKindTypeServiceIT initialized, EventKindTypeServiceIF services: {}", this.eventKindTypeService.getClass().getName());
  }

  @Test
  void testUpvoteEvent() throws NostrException, NoSuchAlgorithmException {
    Identity identity = Identity.generateRandomIdentity();
    PublicKey upvotedUser = Identity.generateRandomIdentity().getPublicKey();

    BadgeAwardUpvoteEvent upvoteEvent = new BadgeAwardUpvoteEvent(identity, upvotedUser);
    GenericEventKindTypeIF genericEventKindIF = new GenericEventKindTypeDto(upvoteEvent, AfterimageKindType.UPVOTE).convertBaseEventToGenericEventKindTypeIF();
    eventKindTypeService.processIncomingEvent(genericEventKindIF);
  }

  @Test
  void testDownvoteEvent() throws NostrException, NoSuchAlgorithmException {
    Identity identity = Identity.generateRandomIdentity();
    Identity downvotedUser = Identity.generateRandomIdentity();

    BadgeAwardDownvoteEvent downvoteEvent = new BadgeAwardDownvoteEvent(identity, downvotedUser);
    GenericEventKindTypeIF genericEventKindIF = new GenericEventKindTypeDto(downvoteEvent, AfterimageKindType.DOWNVOTE).convertBaseEventToGenericEventKindTypeIF();
    eventKindTypeService.processIncomingEvent(genericEventKindIF);
  }

  @Test
  void testTextNoteEvent() throws NostrException, NoSuchAlgorithmException {
    Identity identity = Identity.generateRandomIdentity();

    TextNoteEvent textNoteEvent = new TextNoteEvent(identity, "TEXT note event text content");
    eventKindService.processIncomingEvent(new GenericEventKindDto(textNoteEvent).convertBaseEventToGenericEventKindIF());
  }
}
