package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.SingleContainerTestConfig;
import com.prosilion.afterimage.service.event.plugin.AbstractVoteEventPlugin;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(SingleContainerTestConfig.class)
public class SuperconductorSingleEventThenAfterimageReqIT extends AbstractIT {

  @Autowired
  public SuperconductorSingleEventThenAfterimageReqIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) throws ParseException, InterruptedException {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrl);
  }

  @Test
  void aSuperconductorEventThenAfterimageReq() throws NostrException {
    submitAimgEvent(
       submitSCEvent(
          createUpvoteEvent(superconductorRelay),
          superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey())));

    assertEquals(
       "1",
       submitAfterImageReq(upvoteDefnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl).getFirst().getContent());
  }

  @Test
  void bSuperconductorEventEmptyAddressTagRelayTriesSourceRelayThenAfterimageReq() throws NostrException {
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> event = new BadgeAwardGenericEvent<>(
       submitter, recipient.getPublicKey(), superconductorRelay, awardUpvoteDefinitionEvent);

    submitAimgEvent(
       submitSCEvent(
          AbstractVoteEventPlugin.emptyAddressTagRelayTriesSourceRelay(
             event, event.getAddressTag(), awardUpvoteDefinitionEvent),
          superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey())));

    assertEquals(
       "2",
       submitAfterImageReq(upvoteDefnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl).getFirst().getContent());
  }
}
