package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.config.MultiContainerSameRelayTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.util.Util;
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

import static com.prosilion.afterimage.config.ContainerTestConfig.SUPERCONDUCTOR_AFTERIMAGE;

/**
 * test name "SearchRelaysListRelaySetsSameRelay" means:
 * BadgeDefinitionReputationEvent and SearchRelaysListEvent for same (5556) aImg
 * note: varies from {@link SearchRelaysListDockerRelayIT}, which is docker 5557
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(MultiContainerSameRelayTestConfig.class)
public class SearchRelaysListSameRelayIT extends AbstractIT {
  CacheServiceIF cacheServiceIF;

  @Autowired
  public SearchRelaysListSameRelayIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull CacheServiceIF cacheServiceIF,
     @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl) throws InterruptedException {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrl);
    this.cacheServiceIF = cacheServiceIF;
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent =
       new BadgeAwardGenericEvent<>(
          submitter,
          recipient.getPublicKey(),
          awardUpvoteDefinitionEvent,
          String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", recipient.getPublicKey()),
          new Relay("ws://localhost:5555"));

    submitRelayEvent(badgeAwardUpvoteEvent, superconductorRelayUrl);
    TimeUnit.MILLISECONDS.sleep(1000);

    Util.debug(log, "SearchRelaysListSameRelayIT - watch SC for incoming request search relays within next", "5 seconds", true, 'A');
    TimeUnit.MILLISECONDS.sleep(5_000);
    submitRelayEvent(
       createSearchRelaysListEventMessage(),
       afterimageRelayUrl);
  }

  @Test
  void searchRelaysListRelaySetsSameRelay() throws NostrException, InterruptedException {
//    Util.debug(log, "start wait search relays list processing, including 1st upvote event", "12 seconds", true, '2');
    TimeUnit.MILLISECONDS.sleep(12_000); // time window aImg process badgeAwardEvent
//    Util.debug(log, "end wait search relays list processing, including 1st upvote event", "12 seconds", true, '3');
    RequestSubscriber<BaseMessage> subscriber_1 = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl, subscriber_1);
    validateSpecificAfterimageRequestResults(subscriber_1, 1, "1");

//    submit 2nd SC upvote event
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent = new BadgeAwardGenericEvent<>(
       submitter,
       recipient.getPublicKey(),
       awardUpvoteDefinitionEvent,
       String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", recipient.getPublicKey()),
       new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555"));
    submitRelayEventWithDuration_backup(badgeAwardUpvoteEvent, superconductorRelayUrl);
    Util.debug(log, "start wait SC 2nd upvote event propagates to aimg", "12 seconds", true, '4');
    TimeUnit.MILLISECONDS.sleep(12_000); // time window aImg process badgeAwardEvent
    Util.debug(log, "end wait 2nd upvote event propagates to aimg", "12 seconds", true, '5');

    //  check subscriber_1 has received updated score        
    validateSpecificAfterimageRequestResults(subscriber_1, 1, "2");

//  intro 2nd subscriber    
    RequestSubscriber<BaseMessage> subscriber_2 = new RequestSubscriber<>(Duration.ofSeconds(10));
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl, subscriber_2);
    validateSpecificAfterimageRequestResults(subscriber_2, 1, "2");

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardDownvoteEvent = new BadgeAwardGenericEvent<>(
       submitter,
       recipient.getPublicKey(),
       awardDownvoteDefinitionEvent,
       String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", recipient.getPublicKey()),
       new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555"));
    submitRelayEventWithDuration_backup(badgeAwardDownvoteEvent, superconductorRelayUrl);
    Util.debug(log, "start wait SC downvote event propagates to aimg", "12 seconds", true, '6');
    TimeUnit.MILLISECONDS.sleep(12_000); // time window aImg process badgeAwardEvent
    Util.debug(log, "start wait SC downvote event propagates to aimg", "12 seconds", true, '7');

//  check subscriber_1 has received updated score    
    validateSpecificAfterimageRequestResults(subscriber_1, 1, "1");
//  check subscriber_2 has received updated score    
    validateSpecificAfterimageRequestResults(subscriber_2, 1, "1");

    RequestSubscriber<BaseMessage> subscriber_3 = new RequestSubscriber<>(Duration.ofSeconds(10));
    submitAfterImageReqWithSubscriber(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl, subscriber_3);
    validateSpecificAfterimageRequestResults(subscriber_3, 1, "1");
  }
}
