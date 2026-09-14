package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.config.MultiContainerTestConfig;
import com.prosilion.afterimage.service.reactive.abstracts.AbstractDockerRelayIT;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardCanonicalEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
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

import static com.prosilion.afterimage.config.ContainerTestConfig.SUPERCONDUCTOR_AFTERIMAGE;

/**
 * test name "SearchRelaysListRelaySets" means:
 * BadgeDefinitionReputationEvent and SearchRelaysListEvent for docker (5557) aImg relay
 * note: varies from {@link SearchRelaysListSameRelayIT}, which is 5556
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(MultiContainerTestConfig.class)
@TestPropertySource(properties = {
   "superconductor.event.curation.active=true"
})
public class SearchRelaysListDockerRelayIT extends AbstractDockerRelayIT {

  @Autowired
  public SearchRelaysListDockerRelayIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${afterimage.relay.url.two}") String afterimageRelayUrlTwo,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     CacheServiceIF cacheServiceIF) throws InterruptedException {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrlTwo, cacheServiceIF);
  }

  @Test
  void superconductorEventThenAfterimageReq() throws NostrException, InterruptedException {
// aImg_2 sanity check
    TimeUnit.MILLISECONDS.sleep(12_000); // wait aImg process ctor badgeAwardEvent 
    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_A = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()),
       afterimageRelayUrlTwo,
       aImg_2_EventSubscriber_A);

    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_A, 1, "1");

    BadgeAwardCanonicalEvent badgeAwardUpvoteEvent_2 = createUpvoteEventForCanonicalRecipient(new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555"));

//  submit upvote event to SC
    submitRelayEvent_WithDuration(badgeAwardUpvoteEvent_2, superconductorRelayUrl);
    TimeUnit.MILLISECONDS.sleep(12_000); // wait aImg process ctor badgeAwardEvent

// aImg_2 sanity check		
    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_B = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlTwo, aImg_2_EventSubscriber_B);

    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_B, 1, "2");
  }
}
