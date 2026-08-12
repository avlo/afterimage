package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.config.SingleContainerTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import java.util.List;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(SingleContainerTestConfig.class)
public class SuperconductorMultipleEventsThenAfterimageReqIT extends AbstractIT {
  CacheServiceIF cacheServiceIF;
  @Autowired
  public SuperconductorMultipleEventsThenAfterimageReqIT(
     @NonNull CacheServiceIF cacheServiceIF,
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrl);
    this.cacheServiceIF = cacheServiceIF;
  }

  @Test
  void superconductorMultipleEventsThenAfterimageReq() throws NostrException {
    submitAimgEvent(
       submitSCEvent(
          createUpvoteEvent(superconductorRelay),
          superconductorRelayUrl, upvoteAndOrDownvoteEventFilter));

    assertEquals(
       "1",
       submitAfterImageReq(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl).getFirst().getContent());

// second upvote    
    submitAimgEvent(
       submitSCEvent(
          createUpvoteEvent(superconductorRelay),
          superconductorRelayUrl, upvoteAndOrDownvoteEventFilter));

// third upvote    
    submitAimgEvent(
       submitSCEvent(
          createUpvoteEvent(superconductorRelay),
          superconductorRelayUrl, upvoteAndOrDownvoteEventFilter));

    List<EventIF> returnedAfterImageEvents_B = validateGeneralAfterimageRequestResults(
       submitAfterImageReq(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl));

    assertTrue(returnedAfterImageEvents_B.stream().map(EventIF::getContent).anyMatch("3"::equals));
  }
}
