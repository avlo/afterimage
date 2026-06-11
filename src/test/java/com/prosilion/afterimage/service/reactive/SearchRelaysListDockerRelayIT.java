package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.MultiContainerTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.subdivisions.client.RequestSubscriber;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static com.prosilion.afterimage.config.MultiContainerTestConfig.SUPERCONDUCTOR_AFTERIMAGE;

/**
 * test name "SearchRelaysListRelaySets" means:
 * BadgeDefinitionReputationEvent and SearchRelaysListEvent for docker (5557) aImg relay
 * note: varies from {@link SearchRelaysListSameRelayIT}, which is 5556
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(MultiContainerTestConfig.class)
public class SearchRelaysListDockerRelayIT extends AbstractDockerRelayIT {

  @Autowired
  public SearchRelaysListDockerRelayIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${afterimage.relay.url.two}") String afterimageRelayUrlTwo,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl) throws ParseException, InterruptedException {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrlTwo);
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws NostrException, InterruptedException {
// aImg_2 sanity check		
    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_A = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(upvoteDefnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()),
       afterimageRelayUrlTwo,
       aImg_2_EventSubscriber_A);

    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_A, 1, "1");

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent_2 =
       new BadgeAwardGenericEvent<>(
          submitter,
          recipient.getPublicKey(),
          new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555"),
          awardUpvoteDefinitionEvent,
          String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", recipient.getPublicKey()));

//  submit upvote event to SC
    submitRelayEvent(badgeAwardUpvoteEvent_2, superconductorRelayUrl);
    TimeUnit.MILLISECONDS.sleep(1500);

// aImg_2 sanity check		
    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_B = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(upvoteDefnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlTwo, aImg_2_EventSubscriber_B);

    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_B, 1, "2");
  }
}
