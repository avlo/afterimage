package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.config.SingleContainerTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
public class AfterimageReqThenMultipleSuperconductorEventsWithLocalFormulasBadgeDefinitionWithoutRelayTagIT extends AbstractWithoutRelayTagIT {
  private final Relay superconductorRelay = new Relay("ws://localhost:5555");
  private final Relay afterimageRelay = new Relay("ws://localhost:5556");

  @Autowired
  public AfterimageReqThenMultipleSuperconductorEventsWithLocalFormulasBadgeDefinitionWithoutRelayTagIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl,
     @NonNull CacheServiceIF cacheServiceIF) {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrl, cacheServiceIF);
  }

  @Test
  void superconductorEventAddressTagWithoutRelayTriesSourceRelayThenAfterimageReq() throws NostrException, InterruptedException {
    RequestSubscriber<BaseMessage> reputationRequestSubscriber = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl, reputationRequestSubscriber);

    // # --------------------- SC EVENT 1 of 2-------------------
//    begin event creation for submission to SC
    submitAimgEvent(
       submitSCEvent(
          createUpvoteEventForCanonicalRecipient(superconductorRelay),
          superconductorRelayUrl, upvoteAndOrDownvoteEventFilter));

// # --------------------- SC EVENT 2 of 2-------------------
//    begin event creation for submission to SC
    submitAimgEvent(
       submitSCEvent(
          createUpvoteEventForCanonicalRecipient(superconductorRelay),
          superconductorRelayUrl, upvoteAndOrDownvoteEventFilter));

// # --------------------- Aimg EVENTS returned -------------------
    TimeUnit.MILLISECONDS.sleep(1000);

// subscriber gets both events, starting with the first...    
    List<EventIF> eventIFS = validateSpecificAfterimageRequestResults(reputationRequestSubscriber, 2, "1");

// now validate the second    
    assertEquals("2", eventIFS.getLast().getContent());
  }
}
