package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.util.AfterimageRelayReactiveClient;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import java.io.IOException;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.api.NIP01;
import nostr.event.impl.GenericEvent;
import nostr.event.message.EventMessage;
import nostr.event.message.OkMessage;
import nostr.id.Identity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class EventNoOpMessageReactiveIT {
  private final AfterimageRelayReactiveClient afterImageRelayClient;

  @Autowired
  EventNoOpMessageReactiveIT(@NonNull AfterimageRelayReactiveClient afterimageRelayReactiveClient) {
    this.afterImageRelayClient = afterimageRelayReactiveClient;
  }

  @Test
  void testEventNoOpMessage() throws IOException {
    String content = Factory.lorumIpsum(getClass());
    Identity identity = Factory.createNewIdentity();

    GenericEvent genericEvent = new NIP01<>(identity).createTextNoteEvent(content).sign().getEvent();

    log.debug("textNoteEvent getId(): " + genericEvent.getId());
    log.debug("textNoteEvent getPubKey().toHexString(): " + genericEvent.getPubKey().toHexString());
    assertEquals(genericEvent.getPubKey().toHexString(), identity.getPublicKey().toHexString());

    TestSubscriber<OkMessage> okMessageSubscriber = new TestSubscriber<>();
    this.afterImageRelayClient.send(new EventMessage(genericEvent), okMessageSubscriber);
    final String noOpResponse = "application-test.properties afterimage is a nostr-reputation authority relay.  it does not accept events, only requests";

    List<OkMessage> items = okMessageSubscriber.getItems();
    assertEquals(false, items.getFirst().getFlag());
    assertEquals(noOpResponse, items.getFirst().getMessage());
  }
}
