package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.SingleContainerTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.superconductor.base.service.event.EventServiceIF;
import java.io.IOException;
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

@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(SingleContainerTestConfig.class)
public class AfterimageReqThenSingleSuperconductorExplicitEventMessageIT extends AbstractIT {

  @Autowired
  public AfterimageReqThenSingleSuperconductorExplicitEventMessageIT(
      @NonNull EventServiceIF eventServiceIF,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) throws ParseException, InterruptedException {
    super(eventServiceIF, superconductorRelayUrl, afterimageRelayUrl);
  }

  @Test
  void testAfterimageReqThenSuperconductorTwoEvents() throws IOException, NostrException {
//    // # --------------------- Aimg REQ -------------------
//    //   results should process at end of test once SC vote events have completed
    RequestSubscriber<BaseMessage> reputationRequestSubscriber = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(defnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrl, reputationRequestSubscriber);

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> upvoteEvent = createUpvoteEvent(submitter, recipient, superconductorRelay);
    EventIF returnedScEventIF = submitSCEvent(
        upvoteEvent,
        superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey()));

    assertEquals(returnedScEventIF.getContent(), upvoteEvent.getContent());
    assertEquals(returnedScEventIF.getPublicKey(), upvoteEvent.getPublicKey());
    assertEquals(returnedScEventIF.getKind(), upvoteEvent.getKind());

    EventMessage eventMessage = new EventMessage(returnedScEventIF.asGenericEventRecord());
    assertEquals(eventAsJson(eventMessage.getEvent()), eventMessage.encode()); // sanity check

    //    save SC result to Aimg
    //    should trigger Aimg afterImageEventsSubscriber
    eventServiceIF.processIncomingEvent(eventMessage);

    EventIF returnedReputationEventIFs = getGenericEvents(reputationRequestSubscriber.getItems()).getFirst();
    assertEquals(recipient.getPublicKey(), returnedReputationEventIFs.requireFirstTag(PubKeyTag.class).getPublicKey());
  }

  private String eventAsJson(EventIF event) {
    AddressTag addressTag = event.requireFirstTag(AddressTag.class);
    String pubkeyTagString = event.requireFirstTag(PubKeyTag.class).getPublicKey().toString();
    String addressTagString = String.valueOf(
            addressTag.getKind().getValue()).concat(":")
        .concat(
            addressTag.getPublicKey().toString()).concat(":")
        .concat(
            addressTag.getIdentifierTag().getUuid().concat("\",\"")
                .concat(superconductorRelayUrl));

    String s = "[\"EVENT\",{\"id\":\"" + event.getId() + "\",\"pubkey\":\"" + event.getPublicKey() + "\",\"created_at\":" + event.getCreatedAt() + ",\"kind\":" + event.getKind() + ",\"tags\":[" +
        "[\"a\",\"" + addressTagString + "\"]" +
        "," +
        "[\"p\",\"" + pubkeyTagString + "\"]," +
        "[\"relay\",\"" + event.getRelayTagUrl() + "\"]" +
        "],\"content\":\"" + event.getContent() + "\",\"sig\":\"" + event.getSignature() + "\"}]";
    System.out.println(s);
    return s;
  }
}
