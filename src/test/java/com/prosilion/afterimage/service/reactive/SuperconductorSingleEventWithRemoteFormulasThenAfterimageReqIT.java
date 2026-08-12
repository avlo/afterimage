package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.config.SingleContainerTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.AuthorFilter;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
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
public class SuperconductorSingleEventWithRemoteFormulasThenAfterimageReqIT extends AbstractIT {

  @Autowired
  public SuperconductorSingleEventWithRemoteFormulasThenAfterimageReqIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
     @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrl);
  }

  @Test
  void aSuperconductorEventThenAfterimageReq() throws NostrException {
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> upvoteEvent = createUpvoteEvent(superconductorRelay);

    EventIF event = submitSCEvent(
       upvoteEvent,
       superconductorRelayUrl,
       upvoteAndOrDownvoteEventFilter);

    submitRelayEventWithDuration_backup(event, afterimageRelayUrl);

    assertEquals(
       "1",
       submitAfterImageReq(new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl).getFirst().getContent());
  }

  @Test
  void bSuperconductorEventEmptyAddressTagRelayTriesSourceRelayThenAfterimageReq() throws NostrException, InterruptedException {
    BadgeDefinitionGenericEvent awardUpvoteDefinitionEventNullRelay =
       new BadgeDefinitionGenericEvent(upvoteAndOrDownvoteDefnCreator, upvoteIdentifierTag);

    PublicKey scPubKey = new PublicKey("e04e1c1c30df6058433f61681644fd24914f2e02e420496086c61f53eb504c04");
    submitSCEventCheckEventTagEventId(
       awardUpvoteDefinitionEventNullRelay,
       superconductorRelayUrl,
       new Filters(
          new AuthorFilter(scPubKey),
          new KindFilter(Kind.BADGE_DEFINITION_EVENT)),
       scPubKey);

    PublicKey newRecipient = Identity.generateRandomIdentity().getPublicKey();
    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> upvoteEvent =
       new BadgeAwardGenericEvent<>(
          submitter,
          newRecipient,
          awardUpvoteDefinitionEventNullRelay,
          superconductorRelay);

    EventIF submitSCEvent = submitSCEvent(upvoteEvent, superconductorRelayUrl,
       new Filters(
          new ReferencedPublicKeyFilter(new PubKeyTag(newRecipient)),
          new KindFilter(Kind.BADGE_AWARD_EVENT)));

    submitAimgEventWithDuration_backup(submitSCEvent);

    assertEquals(
       "1",
       submitAfterImageReq(new PubKeyTag(newRecipient), afterimageRelayUrl).getFirst().getContent());
  }

  protected EventIF submitSCEventCheckEventTagEventId(BaseEvent event, String url, Filters filters, PublicKey publicKey) {
//  submit first Event to superconductor
    submitRelayEventWithDuration_backup(event, url);
//  sanity check event submissions processed by superconductor
    List<BaseMessage> baseMessages = new NostrSingleRequestService().send(
       createSuperconductorReqMessageEvent(generateRandomHex64String(), filters), url);

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
