package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.SingleContainerTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(SingleContainerTestConfig.class)
public class SuperconductorMultipleEventsThenAfterimageReqIT extends AbstractIT {
  @Autowired
  public SuperconductorMultipleEventsThenAfterimageReqIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) throws ParseException, InterruptedException {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrl);
  }

  @Test
  void superconductorMultipleEventsThenAfterimageReq() throws NostrException {
    submitAimgEvent(
       submitSCEvent(
          createUpvoteEvent(submitter, recipient, superconductorRelay),
          superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey())));

    assertEquals(
       "1",
       submitAfterImageReq(upvoteDefnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl).getFirst().getContent());

// second upvote    
    submitAimgEvent(
       submitSCEvent(
          createUpvoteEvent(submitter, recipient, superconductorRelay),
          superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey())));

// third upvote    
    submitAimgEvent(
       submitSCEvent(
          createUpvoteEvent(submitter, recipient, superconductorRelay),
          superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey())));

    List<EventIF> returnedAfterImageEvents_B = validateGeneralAfterimageRequestResults(
       submitAfterImageReq(upvoteDefnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl));

    assertTrue(returnedAfterImageEvents_B.stream().map(EventIF::getContent).anyMatch("3"::equals));
  }
}
