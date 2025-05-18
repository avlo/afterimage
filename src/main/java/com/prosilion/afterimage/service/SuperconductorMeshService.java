package com.prosilion.afterimage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.ScMeshSubscriber;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.base.PublicKey;
import nostr.event.BaseMessage;
import nostr.event.BaseTag;
import nostr.event.filter.Filters;
import nostr.event.filter.VoteTagFilter;
import nostr.event.impl.GenericEvent;
import nostr.event.message.EventMessage;
import nostr.event.message.ReqMessage;
import nostr.event.tag.PubKeyTag;
import nostr.event.tag.VoteTag;
import nostr.id.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Lazy
@Component
public class SuperconductorMeshService<T extends BaseMessage> {
  public static final String CONTENT = "TEMP LOCATION OF AIMG REPUTATION SCORE";

  private final ReactiveRequestConsolidator requestConsolidator;
  private final ScMeshSubscriber<T> scMeshSubscriber = new ScMeshSubscriber<>();
  private final SuperconductorEventMessageService<EventMessage> eventMessageService;

  private final Identity afterImageIdentity;
  private final String subscriptionId = Factory.generateRandomHex64String();
  private final VoteTag voteTag = new VoteTag(1);

  @Autowired
  public SuperconductorMeshService(
      @NonNull SuperconductorEventMessageService<EventMessage> eventMessageService,
      @NonNull Map<String, String> superconductorRelays) throws JsonProcessingException {
    this.eventMessageService = eventMessageService;
    this.requestConsolidator = new ReactiveRequestConsolidator(superconductorRelays);
    this.afterImageIdentity = Factory.createNewIdentity();
//    setUpReputationReqFlux();
  }

  public void setUpReputationReqFlux() throws JsonProcessingException {
    ReqMessage validReqMsg = new ReqMessage(subscriptionId,
        new Filters(
            new VoteTagFilter<>(voteTag)));
    send(validReqMsg, scMeshSubscriber);
  }

  private void send(@NonNull ReqMessage reqMessage, @NonNull ScMeshSubscriber<T> subscriber) throws JsonProcessingException {
    requestConsolidator.send(reqMessage, subscriber);
    List<T> scReturnedBaseMessages = subscriber.getItems();
    log.debug("SuperconductorMeshService items count {}", scReturnedBaseMessages.size());
    List<EventMessage> eventMessages = filterEventMessages(scReturnedBaseMessages);
    log.debug("SuperconductorMeshService EventMessages count {}", eventMessages.size());
    eventMessages.forEach(this::createReputationEventWithScore);
  }

  private void createReputationEventWithScore(EventMessage eventMessage) {
    log.debug("SuperconductorMeshService EventMessage content: {}", eventMessage);

    List<BaseTag> tags = new ArrayList<>();
    PublicKey publicKey = getPublicKey(eventMessage);
    PubKeyTag e = new PubKeyTag(publicKey);
    tags.add(e);

    GenericEvent textNoteEvent = Factory.createTextNoteEvent(afterImageIdentity, tags, CONTENT);
    textNoteEvent.setKind(2112);
    afterImageIdentity.sign(textNoteEvent);

    EventMessage eventMessage1 = new EventMessage(textNoteEvent);
    eventMessageService.processIncoming(eventMessage1, subscriptionId);
  }

  public void addRelay(String name, String uri) {
    requestConsolidator.addRelay(name, uri);
  }

  public void removeRelay(String name) {
    requestConsolidator.removeRelay(name);
  }

  private List<EventMessage> filterEventMessages(List<T> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .toList();
  }

  private PublicKey getPublicKey(EventMessage eventMessage) {
    GenericEvent event = (GenericEvent) eventMessage.getEvent();
    PublicKey pubKey = event.getPubKey();
    return pubKey;
  }
}
