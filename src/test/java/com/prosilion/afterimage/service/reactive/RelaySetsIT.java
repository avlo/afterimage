package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.config.MultiContainerTestConfig;
import com.prosilion.afterimage.service.reactive.abstracts.AbstractDockerRelayIT;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.RelaySetsEvent;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(MultiContainerTestConfig.class)
@TestPropertySource(properties = {
   "superconductor.event.curation.active=true"
})
public class RelaySetsIT extends AbstractDockerRelayIT {
  private final String afterimageRelayUrlThree;

  @Autowired
  public RelaySetsIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     @NonNull @Value("${afterimage.relay.url.two}") String afterimageRelayUrlTwo,
     @NonNull @Value("${afterimage.relay.url.three}") String afterimageRelayUrlThree,
     CacheServiceIF cacheServiceIF) throws InterruptedException {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrlTwo, cacheServiceIF);
    this.afterimageRelayUrlThree = afterimageRelayUrlThree;
  }

  @Test
  void testFollowSetsEvent() throws InterruptedException {
// aImg_2 sanity check  
    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_A = new RequestSubscriber<>(Duration.ofSeconds(30));
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()),
       afterimageRelayUrlTwo,
       aImg_2_EventSubscriber_A);

    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_A, 1, "1");

//  now notify 5557 (via RELAY SETS EVENT) of 5556's existence
    submitRelayEvent(
       createRelaysSetsEventMessage(), afterimageRelayUrlThree);
    TimeUnit.MILLISECONDS.sleep(5000);  // longer delay

    RequestSubscriber<BaseMessage> aImg_3_EventSubscriber_A = new RequestSubscriber<>(Duration.ofSeconds(30));
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlThree, aImg_3_EventSubscriber_A);

    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_A, 1, "1");

    submitSCEvent(
       createUpvoteEventForCanonicalRecipient(SUPERCONDUCTOR_DOCKER_RELAY),
       superconductorRelayUrl,
       upvoteAndOrDownvoteEventFilter);
    TimeUnit.MILLISECONDS.sleep(12000);  // longer delay

    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_B = new RequestSubscriber<>(Duration.ofSeconds(20));
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlTwo, aImg_2_EventSubscriber_B);

    RequestSubscriber<BaseMessage> aImg_3_EventSubscriber_B = new RequestSubscriber<>(Duration.ofSeconds(20));
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlThree, aImg_3_EventSubscriber_B);

    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_B, 1, "2");
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_B, 1, "2");

    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_A, 1, "2");
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_A, 1, "2");

    submitSCEvent(
       createUpvoteEventForCanonicalRecipient(SUPERCONDUCTOR_DOCKER_RELAY),
       superconductorRelayUrl, upvoteAndOrDownvoteEventFilter);
    TimeUnit.MILLISECONDS.sleep(12000);  // longer delay

    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_B, 1, "3");
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_B, 1, "3");

    submitSCEvent(
       createUpvoteEventForCanonicalRecipient(SUPERCONDUCTOR_DOCKER_RELAY),
       superconductorRelayUrl, upvoteAndOrDownvoteEventFilter);
    TimeUnit.MILLISECONDS.sleep(12000); // longer delay

    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_C = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlTwo, aImg_2_EventSubscriber_C);
    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_C, 1, "4");
  }

  private BaseEvent createRelaysSetsEventMessage() {
    return new RelaySetsEvent(afterimageInstanceIdentity,
       new RelaysTag(AFTERIMAGE_TWO_RELAY),
       "RELAY_SETS_EVENT -> notify 5557 of 5556's existence");
  }
}
