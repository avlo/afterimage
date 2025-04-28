package com.prosilion.afterimage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.util.AfterimageRelayClient;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.superconductor.service.event.EventService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseTag;
import nostr.event.filter.Filters;
import nostr.event.filter.VoteTagFilter;
import nostr.event.impl.GenericEvent;
import nostr.event.message.EventMessage;
import nostr.event.message.ReqMessage;
import nostr.event.tag.VoteTag;
import nostr.id.Identity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class SuperconductorNetworkIT {

  public static ComposeContainer superconductorContainer = new ComposeContainer(
      new File("src/test/resources/superconductor-docker-compose-dev_ws.yml"))
      .withExposedService("superconductor-afterimage", 5555, Wait.forHealthcheck());

  private final AfterimageRelayClient afterImageRelayClient;
  private final EventService<GenericEvent> eventService;

  private final Identity identity = Factory.createNewIdentity();
  private final VoteTag voteTag = new VoteTag(1);
  private final static int KIND = 2112;

  private final static String subscriberId = Factory.generateRandomHex64String();

  @Autowired
  SuperconductorNetworkIT(@NonNull AfterimageRelayClient afterImageRelayClient, @NonNull EventService<GenericEvent> eventService) {
    this.afterImageRelayClient = afterImageRelayClient;
    this.eventService = eventService;
  }

  @Test
  void testReqFilteredByVoteTag() throws JsonProcessingException, InterruptedException {
    String serviceHost = superconductorContainer.getServiceHost("superconductor-afterimage", 5555);
    log.debug("00000000000000000000000");
    log.debug("00000000000000000000000");
    log.debug("serviceHost: {}", serviceHost);
    log.debug("00000000000000000000000");
    log.debug("00000000000000000000000");
    final String CONTENT = Factory.lorumIpsum(SuperconductorNetworkIT.class);

    List<BaseTag> tags = new ArrayList<>();
    tags.add(voteTag);

    GenericEvent textNoteEvent = Factory.createTextNoteEvent(identity, tags, CONTENT);
    textNoteEvent.setKind(KIND);
    identity.sign(textNoteEvent);

    log.debug("textNoteEvent getId(): " + textNoteEvent.getId());
    log.debug("textNoteEvent getPubKey().toHexString(): " + textNoteEvent.getPubKey().toHexString());
    assertEquals(textNoteEvent.getPubKey().toHexString(), identity.getPublicKey().toHexString());

    eventService.processIncomingEvent(new EventMessage(textNoteEvent));
    TimeUnit.SECONDS.sleep(1);

    log.debug("subscriberId testReqFilteredByVoteTag(): " + subscriberId);
    List<GenericEvent> returnedEvents = afterImageRelayClient.sendRequestReturnEvents(
        new ReqMessage(
            subscriberId,
            new Filters(
                new VoteTagFilter<>(voteTag))));

    log.debug("okMessage:");
    log.debug("  " + returnedEvents);
    assertEquals(1, returnedEvents.size());
    assertTrue(returnedEvents.stream().anyMatch(e -> e.getId().equals(textNoteEvent.getId())));
    assertTrue(returnedEvents.stream().anyMatch(e -> e.getPubKey().equals(textNoteEvent.getPubKey())));
    assertTrue(returnedEvents.stream().anyMatch(e -> e.getKind().equals(KIND)));

//    
//    END 1
//    

    final String CONTENT_2 = Factory.lorumIpsum(SuperconductorNetworkIT.class);

    List<BaseTag> tags_2 = new ArrayList<>();
    tags_2.add(voteTag);

    GenericEvent textNoteEvent_2 = Factory.createTextNoteEvent(identity, tags_2, CONTENT_2);
    textNoteEvent_2.setKind(KIND);
    identity.sign(textNoteEvent_2);

    log.debug("textNoteEvent_2 getId(): " + textNoteEvent_2.getId());
    log.debug("textNoteEvent_2 getPubKey().toHexString(): " + textNoteEvent_2.getPubKey().toHexString());
    assertEquals(textNoteEvent_2.getPubKey().toHexString(), identity.getPublicKey().toHexString());

    eventService.processIncomingEvent(new EventMessage(textNoteEvent_2));
    TimeUnit.SECONDS.sleep(1);

    log.debug("subscriberId testReqFilteredBy2ndVoteTag(): " + subscriberId);
    List<GenericEvent> returnedEvents_2 = afterImageRelayClient.updateReqResults(subscriberId);

    log.debug("okMessage:");
    log.debug("  " + returnedEvents_2);

    log.debug("returnedEvents_2: ");
    returnedEvents_2.forEach(e -> log.debug("  " + e));

    assertEquals(1, returnedEvents_2.size());
    assertTrue(returnedEvents_2.stream().anyMatch(e -> e.getId().equals(textNoteEvent_2.getId())));
    assertTrue(returnedEvents_2.stream().anyMatch(e -> e.getPubKey().equals(textNoteEvent_2.getPubKey())));

//    
//    END 2
//      

    List<GenericEvent> returnedEvents_3 = afterImageRelayClient.updateReqResults(subscriberId);
    assertEquals(0, returnedEvents_3.size());
    log.debug("returnedEvents_3: ");
    returnedEvents_3.forEach(e -> log.debug("  " + e));
  }
}
