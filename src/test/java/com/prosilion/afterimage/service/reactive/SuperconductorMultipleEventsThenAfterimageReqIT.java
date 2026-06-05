package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
import com.prosilion.superconductor.base.service.event.EventServiceIF;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
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
public class SuperconductorMultipleEventsThenAfterimageReqIT extends SuperconductorEventsThenAfterimageReqAbstractIT {
  @Autowired
  public SuperconductorMultipleEventsThenAfterimageReqIT(
      @NonNull @Qualifier("eventService") EventServiceIF eventServiceIF,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) throws ParseException, IOException, InterruptedException {
    super(eventServiceIF, superconductorRelayUrl, afterimageRelayUrl);
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException {
    EventIF upvoteEvent = createAndSubmitVoteEvent(superconductorRelayUrl, voteSubmitterIdentity, voteReceierIdentity);
    simulateAimgFollowSetsHandler(upvoteEvent);

//    query Aimg for above badgeAwardUpvoteEvent
    log.debug("query Aimg for above badgeAwardUpvoteEvent:");
    List<BaseMessage> subscriber = new NostrSingleRequestService().send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            voteReceierIdentity.getPublicKey(),
            definitionsCreatorIdentity.getPublicKey()),
        afterimageRelayUrl);

    log.debug("afterimage returned events:");
    List<EventIF> returnedAimgReqGenericEvents_A = getGenericEvents(subscriber);

    assertEquals("1", returnedAimgReqGenericEvents_A.getFirst().getContent());

// second upvote    
    EventIF upvoteEvent2 = createAndSubmitVoteEvent(superconductorRelayUrl, voteSubmitterIdentity, voteReceierIdentity);
    simulateAimgFollowSetsHandler(upvoteEvent2);

// third upvote    
    EventIF upvoteEvent3 = createAndSubmitVoteEvent(superconductorRelayUrl, voteSubmitterIdentity, voteReceierIdentity);
    simulateAimgFollowSetsHandler(upvoteEvent3);

    List<BaseMessage> afterImageEventsSubscriber_B = new NostrSingleRequestService().send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            voteReceierIdentity.getPublicKey(),
            definitionsCreatorIdentity.getPublicKey()),
        afterimageRelayUrl);

    List<EventIF> returnedAfterImageEvents_B = getGenericEvents(afterImageEventsSubscriber_B);

    assertTrue(returnedAfterImageEvents_B.stream().anyMatch(eventIF ->
        eventIF.findFirstTag(PubKeyTag.class).map(PubKeyTag::getPublicKey).stream()
            .anyMatch(publicKey -> publicKey.equals(voteReceierIdentity.getPublicKey()))));

    assertTrue(returnedAfterImageEvents_B.stream().anyMatch(eventIF ->
        eventIF.findFirstTag(AddressTag.class).stream()
            .map(AddressTag::getPublicKey)
            .anyMatch(definitionsCreatorIdentity.getPublicKey()::equals)));

    assertFalse(returnedAfterImageEvents_B.stream().anyMatch(eventIF ->
        eventIF.findFirstTag(AddressTag.class).stream()
            .filter(addressTag -> addressTag.getKind().equals(Kind.BADGE_DEFINITION_EVENT))
            .filter(addressTag -> addressTag.getPublicKey().equals(definitionsCreatorIdentity.getPublicKey()))
            .filter(addressTag -> addressTag.getIdentifierTag().equals(reputationIdentifierTag))
            .toList().isEmpty()));

    assertTrue(returnedAfterImageEvents_B.stream().anyMatch(eventIF ->
        eventIF.findFirstTag(ExternalIdentityTag.class).stream()
            .anyMatch(BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG::equals)));

    assertTrue(returnedAfterImageEvents_B.stream().map(EventIF::getContent).anyMatch("3"::equals));
  }
}
