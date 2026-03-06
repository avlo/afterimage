package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.config.MultiContainerTestConfig;
import com.prosilion.afterimage.util.AfterimageReactiveRelayClient;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.util.RequestSubscriber;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "superconductor.auth.event.kinds=SEARCH_RELAYS_LIST"
//    ,
//    "afterimage.relay.url=ws://localhost:5560",
//    "server.port=5560",
//    "spring.data.redis.port=6390"
})
@Import(MultiContainerTestConfig.class)
public class SearchRelaysListAuthRejectionIT {
  private final Identity afterimageInstanceIdentity;
  private final String afterimageRelayUri;
  Duration requestTimeoutDuration;

  @Autowired
  public SearchRelaysListAuthRejectionIT(
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri,
      @NonNull Identity afterimageInstanceIdentity,
      Duration requestTimeoutDuration) {
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.afterimageRelayUri = afterimageRelayUri;
    this.requestTimeoutDuration = requestTimeoutDuration;
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException, InterruptedException {
    RequestSubscriber<OkMessage> rejectionClient = new RequestSubscriber<>(requestTimeoutDuration);
    final AfterimageReactiveRelayClient aImgSearchRelaysListRejectionSubscriber = new AfterimageReactiveRelayClient(afterimageRelayUri);

    aImgSearchRelaysListRejectionSubscriber.send(
        new EventMessage(
            createSearchRelaysListEventMessage("ws://localhost:1234")),
        rejectionClient);

    TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("afterimage returned superconductor events:");
    OkMessage okRejectionMessage = rejectionClient.getItems().getFirst();
    log.debug("  {}", okRejectionMessage);

    assertFalse(okRejectionMessage.getFlag());
    assertTrue(okRejectionMessage.getMessage().contains("auth-required:"));

    aImgSearchRelaysListRejectionSubscriber.closeSocket();
  }

  private BaseEvent createSearchRelaysListEventMessage(String uri) {
    return new SearchRelaysListEvent(
        afterimageInstanceIdentity,
        new RelaysTag(
            new Relay(uri)),
        "custom SEARCH_RELAYS_LIST content");
  }
}
