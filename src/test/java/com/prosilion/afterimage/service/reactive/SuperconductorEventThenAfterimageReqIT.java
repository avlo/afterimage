package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.util.AfterimageRelayReactiveClient;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.superconductor.service.event.EventService;
import java.io.IOException;
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
import nostr.event.message.OkMessage;
import nostr.event.message.ReqMessage;
import nostr.event.tag.VoteTag;
import nostr.id.Identity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class SuperconductorEventThenAfterimageReqIT {
  private final AfterimageRelayReactiveClient superconductorRelayReactiveClient;
  private final AfterimageRelayReactiveClient afterimageRelayReactiveClient;
  private final EventService<GenericEvent> eventService;

  private final Identity identity = Factory.createNewIdentity();
  private final VoteTag voteTag = new VoteTag(1);
  private final static int KIND = 2112;

  private final static String subscriberId = Factory.generateRandomHex64String();

  @Autowired
  SuperconductorEventThenAfterimageReqIT(
      @NonNull EventService<GenericEvent> eventService,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUri,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri
  ) {
    log.debug("00000000000000000000000");
    log.debug("00000000000000000000000");
    log.debug("superconductorRelayUri: {}", superconductorRelayUri);
    log.debug("-----------------------");
    log.debug("afterimageRelayUri: {}", afterimageRelayUri);
    log.debug("00000000000000000000000");
    log.debug("00000000000000000000000");
    this.superconductorRelayReactiveClient = new AfterimageRelayReactiveClient(superconductorRelayUri);
    this.afterimageRelayReactiveClient = new AfterimageRelayReactiveClient(afterimageRelayUri);
    this.eventService = eventService;
  }

  @Test
  void testSuperconductorEventThenAfterimageReq() throws IOException, InterruptedException {
    final String CONTENT = Factory.lorumIpsum(SuperconductorEventThenAfterimageReqIT.class);

    List<BaseTag> tags = new ArrayList<>();
    tags.add(voteTag);

    GenericEvent textNoteEvent = Factory.createTextNoteEvent(identity, tags, CONTENT);
    textNoteEvent.setKind(KIND);
    identity.sign(textNoteEvent);

    log.debug("textNoteEvent getId(): {}", textNoteEvent.getId());
    log.debug("textNoteEvent getPubKey().toHexString(): {}", textNoteEvent.getPubKey().toHexString());
    assertEquals(textNoteEvent.getPubKey().toHexString(), identity.getPublicKey().toHexString());

//    submit Event to superconductor
    OkMessage okMessage = superconductorRelayReactiveClient.send(new EventMessage(textNoteEvent));
    TimeUnit.SECONDS.sleep(1);
    assertEquals(true, okMessage.getFlag());

//    submit Req for above event to superconductor
    log.debug("subscriberId testReqFilteredByVoteTag():  {}", subscriberId);
    GenericEvent returnedSuperConductorEvent = superconductorRelayReactiveClient.send(
        new ReqMessage(
            subscriberId,
            new Filters(
                new VoteTagFilter<>(voteTag))));

    log.debug("superconductor events:");
    log.debug("  {}", returnedSuperConductorEvent);
    assertEquals(returnedSuperConductorEvent.getId(), textNoteEvent.getId());
    assertEquals(returnedSuperConductorEvent.getPubKey(), textNoteEvent.getPubKey());
    assertEquals(KIND, (int) returnedSuperConductorEvent.getKind());

//    save SC result to Aimg
    eventService.processIncomingEvent(new EventMessage(returnedSuperConductorEvent));

//    query Aimg for above event
    GenericEvent returnAfterimageEvents = afterimageRelayReactiveClient.send(
        new ReqMessage(
            subscriberId,
            new Filters(
                new VoteTagFilter<>(voteTag))));

    log.debug("afterimage returned superconductor events:");
    log.debug("  {}", returnAfterimageEvents);
    assertEquals(returnAfterimageEvents.getId(), textNoteEvent.getId());
    assertEquals(returnAfterimageEvents.getPubKey(), textNoteEvent.getPubKey());
    assertEquals(KIND, (int) returnAfterimageEvents.getKind());
  }
}
