package com.prosilion.afterimage.service;

import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.AfterimageRelayClient;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.message.OkMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class EventNoOpMessageIT {
  private final AfterimageRelayClient afterImageRelayClient;

  private final String authorPubKey;
  private final String eventId;

  @Autowired
  EventNoOpMessageIT(@NonNull AfterimageRelayClient afterImageRelayClient) {
    this.afterImageRelayClient = afterImageRelayClient;
    this.eventId = Factory.generateRandomHex64String();
    this.authorPubKey = Factory.generateRandomHex64String();
  }

  @Test
  void testEventNoOpMessage() throws IOException {
    String content = Factory.lorumIpsum(getClass());
    String globalEventJson =
        "[\"EVENT\",{" +
            "\"id\":\"" + eventId +
            "\",\"kind\":1,\"content\":\"" + content +
            "\",\"pubkey\":\"" + authorPubKey +
            "\",\"created_at\":1717357053050" +
            ",tags:[]" +
            ",sig:\"86f25c161fec51b9e441bdb2c09095d5f8b92fdce66cb80d9ef09fad6ce53eaa14c5e16787c42f5404905536e43ebec0e463aee819378a4acbe412c533e60546\"}]";
    log.debug("setup() send event:\n  {}", globalEventJson);

    OkMessage okMessage = this.afterImageRelayClient.sendEvent(globalEventJson);
    final String noOpResponse = "application-test.properties afterimage is a nostr-reputation authority relay.  it does not accept events, only requests";

    assertEquals(false, okMessage.getFlag());
    assertEquals(noOpResponse, okMessage.getMessage());
  }
}
