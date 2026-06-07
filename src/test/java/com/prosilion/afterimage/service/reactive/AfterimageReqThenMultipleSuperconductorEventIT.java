package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.SingleContainerTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.superconductor.base.service.event.EventServiceIF;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(SingleContainerTestConfig.class)
public class AfterimageReqThenMultipleSuperconductorEventIT extends AbstractIT {
  @Autowired
  public AfterimageReqThenMultipleSuperconductorEventIT(
      @NonNull @Qualifier("eventService") EventServiceIF eventServiceIF,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) throws ParseException, InterruptedException {
    super(eventServiceIF, superconductorRelayUrl, afterimageRelayUrl);
  }

  @Test
  void afterimageReqThenMultipleSuperconductorEvents() throws IOException, NostrException, InterruptedException {
    RequestSubscriber<BaseMessage> reputationRequestSubscriber = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(defnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl, reputationRequestSubscriber);

// # --------------------- SC EVENT 1 of 2-------------------
//    begin event creation for submission to SC
    simulateIncomingFollowSetsEventToAimg(
        submitSCEvent(
            createUpvoteEvent(submitter, recipient, superconductorRelay),
            superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey())));

// # --------------------- SC EVENT 2 of 2-------------------
//    begin event creation for submission to SC
    simulateIncomingFollowSetsEventToAimg(
        submitSCEvent(
            createUpvoteEvent(submitter, recipient, superconductorRelay),
            superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey())));

// # --------------------- Aimg EVENTS returned -------------------
    TimeUnit.MILLISECONDS.sleep(1000);

// subscriber gets both events, starting with the first...    
    List<EventIF> eventIFS = validateSpecificAfterimageRequestResults(reputationRequestSubscriber, 2, "1");

// now validate the second    
    assertEquals("2", eventIFS.getLast().getContent());
  }
}
