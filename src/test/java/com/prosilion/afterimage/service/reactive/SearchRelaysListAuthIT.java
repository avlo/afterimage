package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@EmbeddedRedisStandalone
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "superconductor.auth.event.kinds=SEARCH_RELAYS_LIST",
    "afterimage.relay.url=ws://localhost:5560",
    "server.port=5560",
    "spring.data.redis.port=6390"
})
public class SearchRelaysListAuthIT {
  private final Identity afterimageInstanceIdentity;
  private final String afterimageRelayUri;

  @Autowired
  public SearchRelaysListAuthIT(
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri,
      @NonNull Identity afterimageInstanceIdentity) {
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.afterimageRelayUri = afterimageRelayUri;
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException, NoSuchAlgorithmException, InterruptedException {
    TestSubscriber<OkMessage> rejectionClient = new TestSubscriber<>();
    final AfterimageMeshRelayService aImgSearchRelaysListRejectionSubscriber = new AfterimageMeshRelayService(afterimageRelayUri);

    aImgSearchRelaysListRejectionSubscriber.send(
        new EventMessage(
            createSearchRelaysListEventMessage("ws://localhost:5560")),
        rejectionClient);

    TimeUnit.MILLISECONDS.sleep(1000);

    log.debug("afterimage returned superconductor events:");
    OkMessage okRejectionMessage = rejectionClient.getItems().getFirst();
    log.debug("  {}", okRejectionMessage);

    assertFalse(okRejectionMessage.getFlag());
    assertTrue(okRejectionMessage.getMessage().contains("auth-required:"));

    aImgSearchRelaysListRejectionSubscriber.closeSocket();
  }

  private BaseEvent createSearchRelaysListEventMessage(String uri) throws NoSuchAlgorithmException {
    return new SearchRelaysListEvent(
        afterimageInstanceIdentity,
        "Kind.SEARCH_RELAYS_LIST",
        new RelayTag(
            new Relay(uri)));
  }
}
