package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.relay.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.TextNoteEvent;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.lib.jpa.dto.GenericEventKindDto;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class EventNoOpMessageReactiveIT {
  private final AfterimageMeshRelayService afterImageRelayClient;

  @Autowired
  EventNoOpMessageReactiveIT(@NonNull AfterimageMeshRelayService afterimageMeshRelayService) {
    this.afterImageRelayClient = afterimageMeshRelayService;
  }

  @Test
  void testEventNoOpMessage() throws IOException, NostrException, NoSuchAlgorithmException {
    Identity identity = Identity.generateRandomIdentity();

    TextNoteEvent genericEvent = new TextNoteEvent(identity, "TEXT note event text content");

    log.debug("textNoteEvent getId(): {}", genericEvent.getId());
    log.debug("textNoteEvent getPubKey().toHexString(): {}", genericEvent.getPublicKey().toHexString());
    assertEquals(genericEvent.getPublicKey().toHexString(), identity.getPublicKey().toHexString());

    TestSubscriber<OkMessage> okMessageSubscriber = new TestSubscriber<>();
    this.afterImageRelayClient.send(new EventMessage(
        new GenericEventKindDto(genericEvent).convertBaseEventToGenericEventKindIF()), okMessageSubscriber);
    final String noOpResponse = "application-test.properties afterimage is a nostr-reputation authority relay.  it does not accept events, only requests";

    List<OkMessage> items = okMessageSubscriber.getItems();
    assertEquals(false, items.getFirst().getFlag());
    assertEquals(noOpResponse, items.getFirst().getMessage());
  }
}
