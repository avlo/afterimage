package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.SingleContainerTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.CurationSetsEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(SingleContainerTestConfig.class)
public class SuperconductorSingleEventThenAfterimageReqIT extends AbstractIT {
  private final CacheServiceIF cacheServiceIF; // convenience for debug testing, aka getAll()

  @Autowired
  public SuperconductorSingleEventThenAfterimageReqIT(
     @NonNull CacheServiceIF cacheServiceIF,
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) throws ParseException, InterruptedException {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrl);
    this.cacheServiceIF = cacheServiceIF;
  }

  @Test
  void aSuperconductorEventThenAfterimageReq() throws NostrException {
    EventIF event = submitSCEvent(
       createUpvoteEvent(superconductorRelay),
       superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey()));

    submitRelayEventWithDuration_backup(event, afterimageRelayUrl);

    EventTag eventTag = new EventTag(event.getId(), event.getRelayTag().map(RelayTag::getRelay).map(Relay::getUrl).orElseThrow());

    CurationSetsEvent curationSetsEvent = new CurationSetsEvent(
       afterimageInstanceIdentity,
       event.getPublicKey(),
       awardUpvoteDefinitionEvent.getIdentifierTag(),
       awardDownvoteDefinitionEvent.asAddressableEventAddressTag(),
       eventTag,
       superconductorRelay);

//    submitRelayEventWithDuration_backup(curationSetsEvent, afterimageRelayUrl);

    assertEquals(
       "1",
       submitAfterImageReq(repDefnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl).getFirst().getContent());
  }

  @Test
  void bSuperconductorEventEmptyAddressTagRelayTriesSourceRelayThenAfterimageReq() throws NostrException {
    BadgeDefinitionGenericEvent awardUpvoteDefinitionEventNullRelay =
       new BadgeDefinitionGenericEvent(upvoteDefnCreator, upvoteIdentifierTag);

    PublicKey scPubKey = new PublicKey("e04e1c1c30df6058433f61681644fd24914f2e02e420496086c61f53eb504c04");
    submitSCEventCheckEventTagEventId(
       awardUpvoteDefinitionEventNullRelay,
       superconductorRelayUrl,
       badgeDefinitionEventFilter.apply(scPubKey),
       scPubKey);

    PublicKey newRecipient = Identity.generateRandomIdentity().getPublicKey();
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> upvoteEvent =
       new BadgeAwardGenericEvent<>(
          submitter,
          newRecipient,
          awardUpvoteDefinitionEventNullRelay,
          superconductorRelay);

    EventIF submitSCEvent = submitSCEvent(
       upvoteEvent,
       superconductorRelayUrl,
       badgeAwardEventFilter.apply(newRecipient));

    submitAimgEventWithDuration_backup(submitSCEvent);

    assertEquals(
       "1",
       submitAfterImageReq(upvoteDefnCreator.getPublicKey(), new PubKeyTag(newRecipient), afterimageRelayUrl).getFirst().getContent());
  }

  protected EventIF submitSCEventCheckEventTagEventId(BaseEvent event, String url, Filters filters, PublicKey publicKey) {
//  submit first Event to superconductor
    submitRelayEvent(event, url);
//  sanity check event submissions processed by superconductor
    List<BaseMessage> baseMessages = new NostrSingleRequestService().send(
       createSuperconductorReqMessageEvent(generateRandomHex64String(), filters), url);

    // TimeUnit.MILLISECONDS.sleep(2500);
    log.debug("retrieved superconductor events:");
    List<EventIF> receivedEventIFs = getGenericEvents(baseMessages);
    receivedEventIFs.stream().map(EventIF::asGenericEventRecord).map(GenericEventRecord::createPrettyPrintJson).forEach(log::debug);

    EventIF upvoteEventIF = receivedEventIFs.getFirst();

    assertEquals(event.getId(), upvoteEventIF.requireFirstTag(EventTag.class).getEventId());
    assertEquals(publicKey, upvoteEventIF.getPublicKey());
    assertEquals(upvoteEventIF.getContent(), event.getContent());
//    assertEquals(upvoteEventIF.getPublicKey().toHexString(), event.getPublicKey().toHexString());
    assertEquals(upvoteEventIF.getKind(), event.getKind());

    return upvoteEventIF;
  }
}
