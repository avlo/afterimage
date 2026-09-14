package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.config.SingleContainerTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardCanonicalEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.nostr.util.Util;
import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import java.util.List;
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
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(SingleContainerTestConfig.class)
@TestPropertySource(properties = {
   "superconductor.event.curation.active=true"
})
public class SuperconductorSingleEventWithLocalFormulasBadgeDefinitionWithoutRelayTagThenAfterimageReqIT extends AbstractWithoutRelayTagIT {
  private final Relay superconductorRelay = new Relay("ws://localhost:5555");
  private final Relay afterimageRelay = new Relay("ws://localhost:5556");

  @Autowired
  public SuperconductorSingleEventWithLocalFormulasBadgeDefinitionWithoutRelayTagThenAfterimageReqIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl,
     @NonNull CacheServiceIF cacheServiceIF) {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrl, cacheServiceIF);
  }

  @Test
  void bSuperconductorEventAddressTagWithoutRelayTriesSourceRelayThenAfterimageReq() throws NostrException {
    BadgeAwardCanonicalEvent upvoteEvent = createUpvoteEventForCanonicalRecipient(superconductorRelay);

    EventIF simulateIncomingUpvoteEvent = submitSCEvent(
       upvoteEvent,
       superconductorRelayUrl,
       upvoteAndOrDownvoteEventFilter);

    submitRelayEvent_WithDuration(simulateIncomingUpvoteEvent, afterimageRelayUrl);

    assertEquals(
       "1",
       submitAfterImageReq(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl).getFirst().getContent());
  }

  protected EventIF submitSCEventCheckEventTagEventId(BaseEvent event, String url, Filters filters, PublicKey publicKey) {
//  submit first Event to superconductor
    submitRelayEvent(event, url);
//  sanity check event submissions processed by superconductor
    List<BaseMessage> baseMessages = new NostrSingleRequestService().send(
       createSuperconductorReqMessageEvent(Util.generateRandomHex64String(), filters), url);

    log.debug("retrieved superconductor events:");
    List<EventIF> receivedBadgeDefinitionEventIFs = getEventIFs(baseMessages);
    receivedBadgeDefinitionEventIFs.stream().map(EventIF::asGenericEventRecord).map(GenericEventRecord::createPrettyPrintJson).forEach(log::debug);

    EventIF upvoteBadgeDefinitionEventIF = receivedBadgeDefinitionEventIFs.getFirst();

    assertEquals(publicKey, upvoteBadgeDefinitionEventIF.getPublicKey());
    assertEquals(upvoteBadgeDefinitionEventIF.getContent(), event.getContent());
    assertEquals(upvoteBadgeDefinitionEventIF.getKind(), event.getKind());

    return upvoteBadgeDefinitionEventIF;
  }
}
