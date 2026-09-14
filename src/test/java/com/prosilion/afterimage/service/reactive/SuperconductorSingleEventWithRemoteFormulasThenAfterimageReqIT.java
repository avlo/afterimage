package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.config.SingleContainerTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardCanonicalEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(SingleContainerTestConfig.class)
@TestPropertySource(properties = {
   "superconductor.event.curation.active=true"
})
public class SuperconductorSingleEventWithRemoteFormulasThenAfterimageReqIT extends AbstractWithRelaysTagIT {

  @Autowired
  public SuperconductorSingleEventWithRemoteFormulasThenAfterimageReqIT(@NonNull Identity afterimageInstanceIdentity, @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl, @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl, CacheServiceIF cacheServiceIF) {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrl, cacheServiceIF);
  }

  @Test
  void superconductorEventThenAfterimageReq() throws NostrException {
    BadgeAwardCanonicalEvent upvoteEvent = createUpvoteEventForCanonicalRecipient(superconductorRelay);
    EventIF event = submitSCEvent(upvoteEvent, superconductorRelayUrl, upvoteAndOrDownvoteEventFilter);
    submitRelayEvent_WithDuration(event, afterimageRelayUrl);
    assertEquals("1", submitAfterImageReq(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl).getFirst().getContent());
  }
}
