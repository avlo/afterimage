package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.nostr.NostrException;
import com.prosilion.superconductor.base.service.event.EventServiceIF;
import java.io.IOException;
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
public class SuperconductorSingleEventThenAfterimageReqIT extends AbstractIT {

  @Autowired
  public SuperconductorSingleEventThenAfterimageReqIT(
      @NonNull @Qualifier("eventService") EventServiceIF eventServiceIF,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) throws ParseException, IOException, InterruptedException {
    super(eventServiceIF, superconductorRelayUrl, afterimageRelayUrl);
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws IOException, NostrException {
    simulateAimgFollowSetsHandler(
        createAndSubmitVoteEvent(superconductorRelayUrl, voteSubmitterIdentity, voteReceierIdentity));

    assertEquals(
        "1",
        submitAfterImageReq(voteReceierIdentity, definitionsCreatorIdentity, afterimageRelayUrl).getFirst().getContent());
  }
}
