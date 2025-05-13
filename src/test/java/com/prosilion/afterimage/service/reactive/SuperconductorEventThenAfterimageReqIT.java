package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.service.CommonContainer;
import com.prosilion.afterimage.util.AfterimageRelayReactiveClient;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.superconductor.service.event.EventService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseTag;
import nostr.event.filter.Filters;
import nostr.event.filter.ReferencedPublicKeyFilter;
import nostr.event.filter.VoteTagFilter;
import nostr.event.impl.GenericEvent;
import nostr.event.message.EventMessage;
import nostr.event.message.OkMessage;
import nostr.event.message.ReqMessage;
import nostr.event.tag.PubKeyTag;
import nostr.event.tag.VoteTag;
import nostr.id.Identity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class SuperconductorEventThenAfterimageReqIT extends CommonContainer {
  private final AfterimageRelayReactiveClient superconductorRelayReactiveClient;
  private final AfterimageRelayReactiveClient afterimageRelayReactiveClient;
  private final EventService<GenericEvent> eventService;

  private final VoteTag voteTag = new VoteTag(1);
  private final static int KIND = 2112;

  @Autowired
  SuperconductorEventThenAfterimageReqIT(
      @NonNull EventService<GenericEvent> eventService,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUri,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri) {
//    String serviceHost = superconductorContainer.getServiceHost("superconductor-afterimage", 5555);
//    log.debug("SuperconductorEventThenAfterimageReqIT host: {}", serviceHost);
    log.debug("superconductorRelayUri: {}", superconductorRelayUri);
    log.debug("afterimageRelayUri: {}", afterimageRelayUri);

    this.superconductorRelayReactiveClient = new AfterimageRelayReactiveClient(superconductorRelayUri);
    this.afterimageRelayReactiveClient = new AfterimageRelayReactiveClient(afterimageRelayUri);
    this.eventService = eventService;
  }

  @Test
  void testSuperconductorEventThenAfterimageReq() throws IOException {
    final Identity identity = Factory.createNewIdentity();
    final Identity authorIdentity = Factory.createNewIdentity();
    final String CONTENT = Factory.lorumIpsum(SuperconductorEventThenAfterimageReqIT.class);

    List<BaseTag> tags = new ArrayList<>();
    tags.add(voteTag);
    tags.add(new PubKeyTag(authorIdentity.getPublicKey()));

    GenericEvent textNoteEvent = Factory.createTextNoteEvent(identity, tags, CONTENT);
    textNoteEvent.setKind(KIND);
    identity.sign(textNoteEvent);

    assertEquals(textNoteEvent.getPubKey().toHexString(), identity.getPublicKey().toHexString());

//    submit Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(textNoteEvent), okMessageSubscriber);
    List<OkMessage> items2 = okMessageSubscriber.getItems();
    assertEquals(true, items2.getFirst().getFlag());

    final String subscriberId = Factory.generateRandomHex64String();
//    submit Req for above event to superconductor

    TestSubscriber<GenericEvent> superconductorEventsSubscriber_X = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        new ReqMessage(subscriberId,
            new Filters(
                new ReferencedPublicKeyFilter<>(new PubKeyTag(authorIdentity.getPublicKey())),
                new VoteTagFilter<>(voteTag))),
        superconductorEventsSubscriber_X);

    log.debug("superconductor events:");
    List<GenericEvent> items = superconductorEventsSubscriber_X.getItems();
    log.debug("  {}", items);
    assertEquals(items.getFirst().getId(), textNoteEvent.getId());
    assertEquals(items.getFirst().getPubKey(), textNoteEvent.getPubKey());
    assertEquals(KIND, (int) items.getFirst().getKind());

//    save SC result to Aimg
    eventService.processIncomingEvent(new EventMessage(items.getFirst()));

//    query Aimg for above event
    TestSubscriber<GenericEvent> afterImageEventsSubscriber_Y = new TestSubscriber<>();
    afterimageRelayReactiveClient.send(
        new ReqMessage(subscriberId,
            new Filters(
                new ReferencedPublicKeyFilter<>(new PubKeyTag(authorIdentity.getPublicKey())),
                new VoteTagFilter<>(voteTag))),
        afterImageEventsSubscriber_Y);

    log.debug("afterimage returned superconductor events:");
    List<GenericEvent> items1 = afterImageEventsSubscriber_Y.getItems();
    log.debug("  {}", items1);
    assertEquals(items1.getFirst().getId(), textNoteEvent.getId());
    assertEquals(items1.getFirst().getPubKey(), textNoteEvent.getPubKey());
    assertEquals(KIND, (int) items1.getFirst().getKind());
  }

  @Test
  void testSuperconductorTwoEventsThenAfterimageReq() throws IOException {
    final Identity identity = Factory.createNewIdentity();
    final Identity authorIdentity = Factory.createNewIdentity();

    List<BaseTag> tags = new ArrayList<>();
    tags.add(voteTag);
    tags.add(new PubKeyTag(authorIdentity.getPublicKey()));

    GenericEvent textNoteEvent_1 = Factory.createTextNoteEvent(identity, tags, Factory.lorumIpsum(SuperconductorEventThenAfterimageReqIT.class));
    textNoteEvent_1.setKind(KIND);
    identity.sign(textNoteEvent_1);

    //    submit subscriber's first Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber_1 = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(new EventMessage(textNoteEvent_1), okMessageSubscriber_1);
    List<OkMessage> items1 = okMessageSubscriber_1.getItems();
    assertEquals(true, items1.getFirst().getFlag());
    log.debug("received 1of2 OkMessage...");

    GenericEvent textNoteEvent_2 = Factory.createTextNoteEvent(identity, tags, Factory.lorumIpsum(SuperconductorEventThenAfterimageReqIT.class));
    textNoteEvent_2.setKind(KIND);
    identity.sign(textNoteEvent_2);

//    submit subscriber's second Event to superconductor
    TestSubscriber<OkMessage> okMessageSubscriber_2 = new TestSubscriber<>();

//    okMessageSubscriber_1.dispose();
    superconductorRelayReactiveClient.send(new EventMessage(textNoteEvent_2), okMessageSubscriber_2);

    List<OkMessage> items = okMessageSubscriber_2.getItems();
    assertEquals(true, items.getFirst().getFlag());
    log.debug("received 2of2 OkMessage...");

// # --------------------- REQ -------------------    
//    submit matching author & vote tag Req to superconductor
    String subscriberId = Factory.generateRandomHex64String();

    TestSubscriber<GenericEvent> superConductorEventsSubscriber_W = new TestSubscriber<>();
    superconductorRelayReactiveClient.send(
        new ReqMessage(subscriberId,
            new Filters(
                new ReferencedPublicKeyFilter<>(new PubKeyTag(authorIdentity.getPublicKey())),
                new VoteTagFilter<>(voteTag))), superConductorEventsSubscriber_W);

    List<GenericEvent> superCondutorEvents = superConductorEventsSubscriber_W.getItems();
    superCondutorEvents.forEach(genericEvent -> log.debug(genericEvent.getId()));

    assertTrue(superCondutorEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(textNoteEvent_1.getContent())));
    assertTrue(superCondutorEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(textNoteEvent_2.getContent())));

//    save SC result to Aimg
    superCondutorEvents.forEach(event -> eventService.processIncomingEvent(new EventMessage(event)));


//    query Aimg for (as yet to be impl'd) reputation score event
    TestSubscriber<GenericEvent> afterImageEventsSubscriber_V = new TestSubscriber<>();
    afterimageRelayReactiveClient.send(
        new ReqMessage(
            subscriberId,
            new Filters(
                new ReferencedPublicKeyFilter<>(new PubKeyTag(authorIdentity.getPublicKey())),
                new VoteTagFilter<>(voteTag))), afterImageEventsSubscriber_V);

    List<GenericEvent> afterImageEvents = afterImageEventsSubscriber_V.getItems();
    log.debug("afterimage returned events:");
    afterImageEvents.forEach(genericEvent -> log.debug(genericEvent.getId()));
    assertTrue(afterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(textNoteEvent_1.getContent())));
    assertTrue(afterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(textNoteEvent_2.getContent())));
  }
}
