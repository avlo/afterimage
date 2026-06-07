package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.MultiContainerSameRelayTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.superconductor.base.service.event.EventServiceIF;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(MultiContainerSameRelayTestConfig.class)
public class SearchRelaysListRelaySetsSameRelayIT extends AbstractIT {
  @Autowired
  public SearchRelaysListRelaySetsSameRelayIT(
      @NonNull @Qualifier("eventService") EventServiceIF eventServiceIF,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl) throws ParseException, InterruptedException {
    super(eventServiceIF, superconductorRelayUrl, afterimageRelayUrl);

    submitSCEvent(
        createUpvoteEvent(submitter, recipient, superconductorRelay),
        superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey()));
    TimeUnit.MILLISECONDS.sleep(100);

    submitRelayEvent(
        createSearchRelaysListEventMessage(superconductorRelay),
        afterimageRelayUrl);
    TimeUnit.MILLISECONDS.sleep(1500);
  }

  @Test
  void searchRelaysListRelaySetsSameRelay() throws NostrException, InterruptedException {
    RequestSubscriber<BaseMessage> subscriber_1 = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(defnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl, subscriber_1);
    validateSpecificAfterimageRequestResults(subscriber_1, 1, "1");

//    submit 2nd SC upvote event
    submitSCEvent(
        createUpvoteEvent(submitter, recipient, superconductorRelay),
        superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey()));
    TimeUnit.MILLISECONDS.sleep(2000); // give time for upvoteEvent to propagate to aImg

//  intro 2nd subscriber    
    RequestSubscriber<BaseMessage> subscriber_2 = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(defnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl, subscriber_2);
    validateSpecificAfterimageRequestResults(subscriber_2, 1, "2");

//  check subscriber_1 has received updated score        
    validateSpecificAfterimageRequestResults(subscriber_1, 1, "2");

//    submit 3rd SC event, a downvote
    submitSCEvent(
        createDownvoteEvent(submitter, recipient, superconductorRelay),
        superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey()));
    TimeUnit.MILLISECONDS.sleep(2000); // give time for upvoteEvent to propagate to aImg    

    RequestSubscriber<BaseMessage> subscriber_3 = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(defnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl, subscriber_3);
    validateSpecificAfterimageRequestResults(subscriber_3, 1, "1");

//  check subscriber_2 has received updated score    
    validateSpecificAfterimageRequestResults(subscriber_2, 1, "1");
//  check subscriber_1 has received updated score    
    validateSpecificAfterimageRequestResults(subscriber_1, 1, "1");
  }
}
