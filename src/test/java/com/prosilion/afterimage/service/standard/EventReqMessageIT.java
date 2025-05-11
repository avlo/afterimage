package com.prosilion.afterimage.service.standard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.util.AfterimageRelayStandardClient;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseTag;
import nostr.event.filter.AuthorFilter;
import nostr.event.filter.Filters;
import nostr.event.filter.VoteTagFilter;
import nostr.event.impl.GenericEvent;
import nostr.event.message.ReqMessage;
import nostr.event.tag.VoteTag;
import nostr.id.Identity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DirtiesContext
class EventReqMessageIT {
  private final AfterimageRelayStandardClient afterImageRelayStandardClient;

  private static final Identity IDENTITY = Factory.createNewIdentity();
  private static final VoteTag VOTE_TAG = new VoteTag(1);

  private final static String CONTENT = Factory.lorumIpsum(EventReqMessageIT.class);
  private final static int KIND = 2112;

  private static GenericEvent textNoteEvent;

  @Autowired
  EventReqMessageIT(@NonNull AfterimageRelayStandardClient afterImageRelayStandardClient, @NonNull EventEntityService<GenericEvent> eventEntityService) {
    this.afterImageRelayStandardClient = afterImageRelayStandardClient;

    List<BaseTag> tags = new ArrayList<>();
    tags.add(VOTE_TAG);

    textNoteEvent = Factory.createTextNoteEvent(IDENTITY, tags, CONTENT);
    textNoteEvent.setKind(KIND);
    IDENTITY.sign(textNoteEvent);

    System.out.println("textNoteEvent getId(): " + textNoteEvent.getId());
    System.out.println("textNoteEvent getPubKey().toString(): " + textNoteEvent.getPubKey().toString());
    System.out.println("textNoteEvent getPubKey().toHexString(): " + textNoteEvent.getPubKey().toHexString());
    System.out.println("textNoteEvent getPubKey().toBech32String(): " + textNoteEvent.getPubKey().toBech32String());

    eventEntityService.saveEventEntity(textNoteEvent);
  }

  @Test
  void testReqFilteredByVoteTag() throws JsonProcessingException {
    final String subscriberId = Factory.generateRandomHex64String();

    List<GenericEvent> returnedEvents = afterImageRelayStandardClient.sendRequestReturnEvents(
        new ReqMessage(
            subscriberId,
            new Filters(
                new VoteTagFilter<>(VOTE_TAG))));

    log.debug("okMessage:");
    log.debug("  " + returnedEvents);
    assertTrue(returnedEvents.stream().anyMatch(e -> e.getId().equals(textNoteEvent.getId())));
    assertTrue(returnedEvents.stream().anyMatch(e -> e.getPubKey().equals(textNoteEvent.getPubKey())));
    assertTrue(returnedEvents.stream().anyMatch(e -> e.getKind().equals(KIND)));
  }

  @Test
  void testReqFilteredByAuthor() throws JsonProcessingException {
    final String subscriberId = Factory.generateRandomHex64String();

    List<GenericEvent> returnedEvents = afterImageRelayStandardClient.sendRequestReturnEvents(
        new ReqMessage(
            subscriberId,
            new Filters(
                new AuthorFilter<>(IDENTITY.getPublicKey()))));

    log.debug("okMessage:");
    log.debug("  " + returnedEvents);

    assertTrue(returnedEvents.stream().anyMatch(e -> e.getPubKey().equals(textNoteEvent.getPubKey())));
  }
}
