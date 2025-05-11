package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.service.CommonContainer;
import com.prosilion.afterimage.util.AfterimageRelayReactiveClient;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.superconductor.service.event.EventService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseTag;
import nostr.event.filter.AuthorFilter;
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
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class SuperconductorEventThenAfterimageReqIT extends CommonContainer {
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
    String serviceHost = superconductorContainer.getServiceHost("superconductor-afterimage", 5555);

    log.debug("00000000000000000000000");
    log.debug("00000000000000000000000");
    log.debug("SuperconductorEventThenAfterimageReqIT host: {}", serviceHost);
    log.debug("SuperconductorEventThenAfterimageReqIT hash: {}", superconductorRelayUri.hashCode());
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
                new AuthorFilter<>(identity.getPublicKey()),
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
                new AuthorFilter<>(identity.getPublicKey()),
                new VoteTagFilter<>(voteTag))));

    log.debug("afterimage returned superconductor events:");
    log.debug("  {}", returnAfterimageEvents);
    assertEquals(returnAfterimageEvents.getId(), textNoteEvent.getId());
    assertEquals(returnAfterimageEvents.getPubKey(), textNoteEvent.getPubKey());
    assertEquals(KIND, (int) returnAfterimageEvents.getKind());
  }

  @Test
  void testSuperconductorTwoEventsThenAfterimageReq() throws IOException {
    TestSubscriber<OkMessage> okMessageSubscriber_1 = new TestSubscriber<>();
    
    List<BaseTag> tags = new ArrayList<>();
    tags.add(voteTag);

    GenericEvent textNoteEvent_1 = Factory.createTextNoteEvent(
        identity, tags,
        Factory.lorumIpsum(SuperconductorEventThenAfterimageReqIT.class));
    textNoteEvent_1.setKind(KIND);
    identity.sign(textNoteEvent_1);

//    submit subscriber's first Event to superconductor
    superconductorRelayReactiveClient.sendF(new EventMessage(textNoteEvent_1)).subscribe(okMessageSubscriber_1);
    assertEquals(true, okMessageSubscriber_1
        .getItems()
        .getFirst()
        .getFlag());
    log.debug("!!!!!!!!!!!!!!!!!!!!!!!!!");
    log.debug("!!!!!!!!!!!!!!!!!!!!!!!!!");
    log.debug("received 1of2 OkMessage...");
    
    GenericEvent textNoteEvent_2 = Factory.createTextNoteEvent(
        identity, tags,
        Factory.lorumIpsum(SuperconductorEventThenAfterimageReqIT.class));
    textNoteEvent_2.setKind(KIND);
    identity.sign(textNoteEvent_2);

//    submit subscriber's second Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber_2 = new TestSubscriber<>();
    log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@");
    log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@");
    superconductorRelayReactiveClient.sendF(new EventMessage(textNoteEvent_2))
        .subscribe(okMessageSubscriber_2)
    ;
    log.debug("#########################");
    log.debug("#########################");
    assertEquals(true, okMessageSubscriber_2
        .getItems()
        .getFirst()
        .getFlag());
    log.debug("received 2of2 OkMessage...");
    
// # --------------------- REQ -------------------    
//    submit matching author & vote tag Req to superconductor
    log.debug("subscriberId testReqFilteredByVoteTag():  {}", subscriberId);
    Flux<GenericEvent> returnedSuperConductorEvents = superconductorRelayReactiveClient.sendF(
        new ReqMessage(
            subscriberId,
            new Filters(
                new AuthorFilter<>(identity.getPublicKey()),
                new VoteTagFilter<>(voteTag))));

    log.debug("2222222222222222222222222");
    log.debug("2222222222222222222222222");
    List<GenericEvent> genericEvents = returnedSuperConductorEvents.collectList().block();
    log.debug("3333333333333333333333333");
    log.debug("3333333333333333333333333");

//    save SC result to Aimg
    genericEvents.forEach(event ->
        eventService.processIncomingEvent(new EventMessage(event)));

//    query Aimg for (as yet to be impl'd) reputation score event
    GenericEvent returnAfterimageEvents = afterimageRelayReactiveClient.send(
        new ReqMessage(
            subscriberId,
            new Filters(
                new AuthorFilter<>(identity.getPublicKey()),
                new VoteTagFilter<>(voteTag))));

    log.debug("afterimage returned superconductor events:");
    log.debug("  {}", returnAfterimageEvents);
//    assertEquals(returnAfterimageEvents.getId(), textNoteEvent_1.getId());
//    assertEquals(returnAfterimageEvents.getPubKey(), textNoteEvent_1.getPubKey());
//    assertEquals(KIND, (int) returnAfterimageEvents.getKind());
  }
}
