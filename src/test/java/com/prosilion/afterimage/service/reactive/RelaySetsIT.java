package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.MultiContainerTestConfig;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.RelaySetsEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.subdivisions.client.RequestSubscriber;
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

import static com.prosilion.afterimage.config.MultiContainerTestConfig.AFTERIMAGE_APP_TWO;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(MultiContainerTestConfig.class)
public class RelaySetsIT extends AbstractDockerRelayIT {
  private final String afterimageRelayUrlThree;

  @Autowired
  public RelaySetsIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     @NonNull @Value("${afterimage.relay.url.two}") String afterimageRelayUrlTwo,
     @NonNull @Value("${afterimage.relay.url.three}") String afterimageRelayUrlThree) throws ParseException, InterruptedException {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrlTwo);
    this.afterimageRelayUrlThree = afterimageRelayUrlThree;
  }

  @Test
  void testFollowSetsEvent() throws InterruptedException {
// aImg_2 sanity check  
    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_A = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(upvoteDefnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()),
       afterimageRelayUrlTwo,
       aImg_2_EventSubscriber_A);

    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_A, 1, "1");

//  now notify 5557 (via RELAY SETS EVENT) of 5556's existence
    submitRelayEvent(
       createRelaysSetsEventMessage(), afterimageRelayUrlThree);
    TimeUnit.MILLISECONDS.sleep(2000);

    RequestSubscriber<BaseMessage> aImg_3_EventSubscriber_A = new RequestSubscriber<>(Duration.ofMinutes(5));
    submitAfterImageReqWithSubscriber(upvoteDefnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlThree, aImg_3_EventSubscriber_A);

    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_A, 1, "1");
  }

  private BaseEvent createRelaysSetsEventMessage() {
    return new RelaySetsEvent(afterimageInstanceIdentity,
       new RelaysTag(new Relay("ws://" + AFTERIMAGE_APP_TWO + ":5556")),
       "RELAY_SETS_EVENT -> notify 5557 of 5556's existence");
  }
}
