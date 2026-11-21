package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.config.ScOneAimgOneTestConfig;
import com.prosilion.afterimage.enums.AfterimageKindType;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(ScOneAimgOneTestConfig.class)
@TestPropertySource(properties = {
   "superconductor.relay.url=ws://172.17.0.1:5555",
   "afterimage.relay.url.three=ws://172.17.0.1:5556",
   "afterimage.relay.url.two=ws://172.19.0.5:5556"
})
public class RelaySetsMultipleRelaysRxRIT extends AbstractDockerRelayIT {
  private static final String appTwo = "172.19.0.5";
  protected static final Relay AFTERIMAGE_TWO_RELAY = new Relay("ws://" + appTwo + ":5556");

  private final String afterimageRelayUrlThree;

  @Autowired
  public RelaySetsMultipleRelaysRxRIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     @NonNull @Value("${afterimage.relay.url.two}") String afterimageRelayUrlTwo,
     @NonNull @Value("${afterimage.relay.url.three}") String afterimageRelayUrlThree,
     CacheServiceIF cacheServiceIF) throws InterruptedException {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrlTwo, cacheServiceIF);
    this.afterimageRelayUrlThree = afterimageRelayUrlThree;
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
    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_A = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()),
       afterimageRelayUrlTwo,
       aImg_2_EventSubscriber_A);

    TimeUnit.MILLISECONDS.sleep(DURATION_SECONDS_NORMAL.toMillis());
    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_A, 1, "1");

//  now notify 5557 (via RELAY SETS EVENT) of 5556's existence
    submitRelayEvent_WithDuration(
       createRelaysSetsEventMessage(), afterimageRelayUrlThree);

    TimeUnit.MILLISECONDS.sleep(DURATION_SECONDS_NORMAL.toMillis());
    RequestSubscriber<BaseMessage> aImg_3_EventSubscriber_A = new RequestSubscriber<>(DURATION_SECONDS_NORMAL);
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlThree, aImg_3_EventSubscriber_A);

    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_A, 1, "1");

    RequestSubscriber<BaseMessage> aImg_3_EventSubscriber_B = new RequestSubscriber<>(DURATION_SECONDS_NORMAL);
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlThree, aImg_3_EventSubscriber_B);
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_B, 1, "1");

    BadgeAwardCanonicalEvent badgeAwardUpvoteEvent_2 = createUpvoteEventForCanonicalRecipient(SUPERCONDUCTOR_DOCKER_RELAY);

    submitSCEvent(
       badgeAwardUpvoteEvent_2,
       superconductorRelayUrl,
       scUpvoteAndOrDownvoteEventFilter);

    TimeUnit.MILLISECONDS.sleep(DURATION_SECONDS_NORMAL.toMillis());
    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_A, 1, "2");
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_A, 1, "2");
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_B, 1, "2");

    RequestSubscriber<BaseMessage> aImg_3_EventSubscriber_C = new RequestSubscriber<>(DURATION_SECONDS_NORMAL);
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlThree, aImg_3_EventSubscriber_C);
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_C, 1, "2");
    BadgeAwardCanonicalEvent badgeAwardUpvoteEvent_3 = createUpvoteEventForCanonicalRecipient(SUPERCONDUCTOR_DOCKER_RELAY);

    submitSCEvent(
       badgeAwardUpvoteEvent_3,
       superconductorRelayUrl,
       scUpvoteAndOrDownvoteEventFilter);

    TimeUnit.MILLISECONDS.sleep(DURATION_SECONDS_NORMAL.toMillis());
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_A, 1, "3");
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_B, 1, "3");
    validateSpecificAfterimageRequestResults(aImg_3_EventSubscriber_C, 1, "3");
  }

  private BaseEvent createRelaysSetsEventMessage() {
    return new RelaySetsEvent(afterimageInstanceIdentity,
       new RelaysTag(new Relay(afterimageRelayUrlTwo)),
       "RELAY_SETS_EVENT -> notify 5557 of 5556's existence");
  }
}
