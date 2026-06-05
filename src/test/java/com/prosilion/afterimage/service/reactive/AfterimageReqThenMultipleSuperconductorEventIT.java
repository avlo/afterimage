package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.SingleContainerTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.ExternalIdentityTag;
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

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) throws ParseException, IOException, InterruptedException {
    super(eventServiceIF, superconductorRelayUrl, afterimageRelayUrl);
  }

  @Test
  void afterimageReqThenMultipleSuperconductorEvents() throws IOException, NostrException, InterruptedException {
    RequestSubscriber<BaseMessage> reputationRequestSubscriber = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(recipient, defnCreator, afterimageRelayUrl, reputationRequestSubscriber);

// # --------------------- SC EVENT 1 of 2-------------------
//    begin event creation for submission to SC
    simulateAimgFollowSetsHandler(
        submitSCEvent(
            createUpvoteEvent(submitter, recipient, superconductorRelay),
            superconductorRelayUrl, recipient));

// # --------------------- SC EVENT 2 of 2-------------------
//    begin event creation for submission to SC
    simulateAimgFollowSetsHandler(
        submitSCEvent(
            createUpvoteEvent(submitter, recipient, superconductorRelay),
            superconductorRelayUrl, recipient));

// # --------------------- Aimg EVENTS returned -------------------
    TimeUnit.MILLISECONDS.sleep(1000);
    List<EventIF> returnedReputationEventIFs = getGenericEvents(reputationRequestSubscriber.getItems());
    log.debug("afterimage returned events:");
    returnedReputationEventIFs.forEach(eventIF -> log.debug(eventIF.getId()));

    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF ->
        eventIF.findFirstTag(PubKeyTag.class).map(PubKeyTag::getPublicKey).stream()
            .anyMatch(recipient.getPublicKey()::equals)));

    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF ->
        eventIF.findFirstTag(AddressTag.class).stream()
            .map(AddressTag::getPublicKey)
            .anyMatch(defnCreator.getPublicKey()::equals)));

    assertFalse(returnedReputationEventIFs.stream().anyMatch(eventIF ->
        eventIF.findFirstTag(AddressTag.class).stream()
            .filter(addressTag -> addressTag.getKind().equals(Kind.BADGE_DEFINITION_EVENT))
            .filter(addressTag -> addressTag.getPublicKey().equals(defnCreator.getPublicKey()))
            .filter(addressTag -> addressTag.getIdentifierTag().equals(reputationIdentifierTag))
            .toList().isEmpty()));

    assertTrue(returnedReputationEventIFs.stream().anyMatch(eventIF ->
        eventIF.findFirstTag(ExternalIdentityTag.class).stream()
            .anyMatch(BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG::equals)));

    assertEquals(2, returnedReputationEventIFs.size());
    assertTrue(returnedReputationEventIFs.stream().map(EventIF::getContent).toList().contains("1"));
    assertTrue(returnedReputationEventIFs.stream().map(EventIF::getContent).toList().contains("2"));
  }
}
