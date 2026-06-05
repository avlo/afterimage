package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.message.BaseMessage;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class SuperconductorSingleEventThenAfterimageReqIT extends SuperconductorEventsThenAfterimageReqAbstractIT {

  @Autowired
  public SuperconductorSingleEventThenAfterimageReqIT(
      @NonNull @Qualifier("eventService") EventServiceIF eventServiceIF,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) throws ParseException, IOException, InterruptedException {
    super(eventServiceIF, superconductorRelayUrl, afterimageRelayUrl);
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException {
    EventIF upvoteEvent = createAndSubmitVoteEvent(superconductorRelayUrl, voteSubmitterIdentity, voteReceierIdentity);
//    simulate Aimg FollowSets handling, inserting 1st SC upvote into aImg
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
  }
}
