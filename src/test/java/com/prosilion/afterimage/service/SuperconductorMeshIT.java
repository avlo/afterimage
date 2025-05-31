package com.prosilion.afterimage.service;

import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.subdivisions.client.reactive.ReactiveEventPublisher;
import com.prosilion.subdivisions.client.reactive.ReactiveRelaySubscriptionsManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.base.PublicKey;
import nostr.event.BaseMessage;
import nostr.event.BaseTag;
import nostr.event.filter.Filters;
import nostr.event.filter.ReferencedPublicKeyFilter;
import nostr.event.impl.GenericEvent;
import nostr.event.message.EventMessage;
import nostr.event.message.OkMessage;
import nostr.event.message.ReqMessage;
import nostr.event.tag.PubKeyTag;
import nostr.event.tag.VoteTag;
import nostr.id.Identity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class SuperconductorMeshIT extends CommonContainer {
  private final ReactiveEventPublisher loneScEventPublisher;
  private final ReactiveRelaySubscriptionsManager loneAimgRequestSubmitterEventReceiver;

  private final VoteTag voteTag = new VoteTag(1);
  private final static int KIND = 2112;


  SuperconductorMeshProxy<BaseMessage> superconductorMeshProxy;


  @Autowired
  SuperconductorMeshIT(
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl,
      @NonNull @Value("${superconductor.lone.relay.url}") String superconductorRelayUri,

      SuperconductorMeshProxy<BaseMessage> superconductorMeshProxy


  ) {
    log.debug("superconductorRelayUri: {}", superconductorRelayUri);

    this.loneScEventPublisher = new ReactiveEventPublisher(superconductorRelayUri);
    this.loneAimgRequestSubmitterEventReceiver = new ReactiveRelaySubscriptionsManager(afterimageRelayUrl);


    this.superconductorMeshProxy = superconductorMeshProxy;
  }

  @Test
  void testSuperconductorEventThenAfterimageReq() throws IOException, InterruptedException {
    final String IRRELEVANT_CONTENT = Factory.lorumIpsum(SuperconductorMeshIT.class);
    final Identity loneScEventPublisherIdentity = Factory.createNewIdentity();
    final PublicKey publicKey = loneScEventPublisherIdentity.getPublicKey();

    List<BaseTag> tags = new ArrayList<>();
    tags.add(voteTag);
//    tags.add(authorReputationPubKeyTag);

    GenericEvent irrelevantTextNoteEvent = Factory.createVoteEvent(loneScEventPublisherIdentity, tags, IRRELEVANT_CONTENT);
    irrelevantTextNoteEvent.setKind(KIND);
    loneScEventPublisherIdentity.sign(irrelevantTextNoteEvent);

//  lone SC publisher submits a vote Event to superconductor
    TestSubscriber<OkMessage> loneScVotePublisher = new TestSubscriber<>();
    loneScEventPublisher.send(new EventMessage(irrelevantTextNoteEvent), loneScVotePublisher);
    List<OkMessage> irrelevantLoneScVoteReturnedMessages = loneScVotePublisher.getItems();
    assertEquals(true, irrelevantLoneScVoteReturnedMessages.getFirst().getFlag());

    /*
     *  window when Aimg discovers and processes above vote event
     */

//  lone Aimg subscriber submits reputation request of same PubKey to Aimg
    final String loneAimgReputationSubscriberId = Factory.generateRandomHex64String();

    TestSubscriber<BaseMessage> loneAimgReputationSubscriberReturnedMessages = new TestSubscriber<>();

    loneAimgRequestSubmitterEventReceiver.send(
        new ReqMessage(loneAimgReputationSubscriberId,
            new Filters(
                new ReferencedPublicKeyFilter<>(new PubKeyTag(publicKey)))),
        loneAimgReputationSubscriberReturnedMessages);

    log.debug("superconductor events:");
    List<BaseMessage> returnedBaseMessages = loneAimgReputationSubscriberReturnedMessages.getItems();
    List<GenericEvent> returnedAfterImageEvents = getGenericEvents(returnedBaseMessages);

//    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> genericEvent.getContent().equals(VoteEventTypePlugin.CONTENT)));
    assertTrue(returnedAfterImageEvents.stream().anyMatch(genericEvent -> getPubKeyTag(genericEvent).stream().map(PublicKey::toHexString).anyMatch(stringStream -> stringStream.equals(publicKey.toHexString()))));
    assertEquals(KIND, returnedAfterImageEvents.getFirst().getKind());
  }

  public static <T extends BaseMessage> List<GenericEvent> getGenericEvents(List<T> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .map(GenericEvent.class::cast)
        .toList();
  }


  List<PublicKey> getPubKeyTag(GenericEvent genericEvent) {
    return genericEvent.getTags().stream()
        .filter(PubKeyTag.class::isInstance)
        .map(PubKeyTag.class::cast)
        .map(PubKeyTag::getPublicKey)
        .toList();
  }
}
