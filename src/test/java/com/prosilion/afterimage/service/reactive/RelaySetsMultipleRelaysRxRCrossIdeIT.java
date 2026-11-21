package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.config.RxRCrossIdeTestConfig;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.service.reactive.abstracts.AbstractDockerRelayCrossIdeIT;
import com.prosilion.afterimage.service.reactive.abstracts.AbstractDockerRelayIT;
import com.prosilion.afterimage.util.EventAttributesMap;
import com.prosilion.nostr.event.BadgeAwardCanonicalEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.RelaySetsEvent;
import com.prosilion.nostr.event.curated.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(RxRCrossIdeTestConfig.class)
@TestPropertySource(properties = {
   "superconductor.relay.url=ws://localhost:5555",
   "afterimage.relay.url.two=ws://localhost:5556", // maven instance
   "afterimage.relay.url.three=ws://localhost:5557" // our/this instance
//   "afterimage.relay.url.two=ws://afterimage-app-two:5556"
//   "afterimage.relay.url.two=ws://localhost:5557"
})
public class RelaySetsMultipleRelaysRxRCrossIdeIT extends AbstractDockerRelayCrossIdeIT {
  public static final String appTwoHost = "localhost";
  public static final String appTwoPort = ":5556";
  public static final Relay AFTERIMAGE_TWO_RELAY = new Relay("ws://" + appTwoHost + appTwoPort);
  
  private final String afterimageRelayUrlThreeIsLocal;

  @Autowired
  public RelaySetsMultipleRelaysRxRCrossIdeIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     @NonNull @Value("${afterimage.relay.url.two}") String afterimageRelayUrlTwo,
     @NonNull @Value("${afterimage.relay.url.three}") String afterimageRelayUrl,
     CacheServiceIF cacheServiceIF) throws InterruptedException {
     super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrlTwo, cacheServiceIF);
    this.afterimageRelayUrlThreeIsLocal = afterimageRelayUrl;
//    this.afterimageRelayUrlThree = "ws://localhost:5556";
  }

  @Override
  protected BadgeDefinitionReputationEvent createBadgeDefinitionReputationEvent() {
    return new BadgeDefinitionReputationEvent(
       repDefnCreator,
       afterimageInstanceIdentity.getPublicKey(),
       reputationIdentifierTag,
       AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       AFTERIMAGE_TWO_RELAY,
       EventAttributesMap.asEventList(this.dbCuratedFormulaEventList));
  }

  @Test
  void testFollowSetsEvent() throws InterruptedException {
    TimeUnit.MILLISECONDS.sleep(DURATION_SECONDS_NORMAL.toMillis());
    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_A = new RequestSubscriber<>(DURATION_MINUTES_DEBUG);
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()),
       afterimageRelayUrlTwo,
       aImg_2_EventSubscriber_A);

    TimeUnit.MILLISECONDS.sleep(DURATION_SECONDS_NORMAL.toMillis());
    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_A, 1, "1");

//    boolean manualVal = false;
//    while (manualVal) {
//      TimeUnit.MILLISECONDS.sleep(2000);
//    }
//  now notify 5557 (via RELAY SETS EVENT) of 5556's existence
    submitRelayEvent_WithDuration(
       createRelaysSetsEventMessage(), afterimageRelayUrlThreeIsLocal);

//    manualVal = true;
//    while (manualVal) {
//      TimeUnit.MILLISECONDS.sleep(2000);
//    }
    TimeUnit.MILLISECONDS.sleep(DURATION_SECONDS_NORMAL.toMillis());
    RequestSubscriber<BaseMessage> aImg_3_EventSubscriber_A = new RequestSubscriber<>(DURATION_MINUTES_DEBUG);
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlThreeIsLocal, aImg_3_EventSubscriber_A);

//    manualVal = false;
//    while (manualVal) {
//      TimeUnit.MILLISECONDS.sleep(2000);
//    }
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_A, 1, "1");

//    manualVal = false;
//    while (manualVal) {
//      TimeUnit.MILLISECONDS.sleep(2000);
//    }
    RequestSubscriber<BaseMessage> aImg_3_EventSubscriber_B = new RequestSubscriber<>(DURATION_MINUTES_DEBUG);
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlThreeIsLocal, aImg_3_EventSubscriber_B);

//    manualVal = false;
//    while (manualVal) {
//      TimeUnit.MILLISECONDS.sleep(2000);
//    }
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_B, 1, "1");

    BadgeAwardCanonicalEvent badgeAwardUpvoteEvent_2 = createUpvoteEventForCanonicalRecipient(SUPERCONDUCTOR_DOCKER_RELAY);

    submitSCEvent(
       badgeAwardUpvoteEvent_2,
       superconductorRelayUrl,
       scUpvoteAndOrDownvoteEventFilter);

//    manualVal = true;
//    while (manualVal) {
//      TimeUnit.MILLISECONDS.sleep(2000);
//    }
    TimeUnit.MILLISECONDS.sleep(DURATION_SECONDS_NORMAL.toMillis());
    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_A, 1, "2");
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_A, 1, "2");
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_B, 1, "2");

    RequestSubscriber<BaseMessage> aImg_3_EventSubscriber_C = new RequestSubscriber<>(DURATION_MINUTES_DEBUG);
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlThreeIsLocal, aImg_3_EventSubscriber_C);

//    manualVal = true;
//    while (manualVal) {
//      TimeUnit.MILLISECONDS.sleep(2000);
//    }
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_C, 1, "2");
//
    BadgeAwardCanonicalEvent badgeAwardUpvoteEvent_3 = createUpvoteEventForCanonicalRecipient(SUPERCONDUCTOR_DOCKER_RELAY);
    
// monitor tmux before submitting below event to see if sleep 5 seconds is low/high    
    submitSCEvent(
       badgeAwardUpvoteEvent_3,
       superconductorRelayUrl,
       scUpvoteAndOrDownvoteEventFilter);

//    manualVal = true;
//    while (manualVal) {
//      TimeUnit.MILLISECONDS.sleep(2000);
//    }
    TimeUnit.MILLISECONDS.sleep(DURATION_SECONDS_NORMAL.toMillis());
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_A, 1, "3");
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_B, 1, "3");
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_C, 1, "3");
  }

  private BaseEvent createRelaysSetsEventMessage() {
    return new RelaySetsEvent(afterimageInstanceIdentity,
//       new RelaysTag(new Relay("ws://" + AFTERIMAGE_APP_TWO + ":5556")),
       new RelaysTag(new Relay(afterimageRelayUrlTwo)),
       "RELAY_SETS_EVENT -> notify 5557 of 5556's existence");
  }
}
