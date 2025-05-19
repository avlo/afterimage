package com.prosilion.afterimage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BaseSubscriber;

@Slf4j
@Service
public class SuperconductorMeshService<T extends BaseMessage> extends BaseSubscriber<T> {
  public static final String CONTENT = "TEMP LOCATION OF AIMG REPUTATION SCORE";

  private final ReactiveRequestConsolidator requestConsolidator;
  private final SuperconductorEventMessageService<EventMessage> eventMessageService;

  private Subscription subscription;

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
    setUpReputationReqFlux();
  }

  public void setUpReputationReqFlux() throws JsonProcessingException {
    ReqMessage validReqMsg = new ReqMessage(subscriptionId,
        new Filters(
            new VoteTagFilter<>(voteTag)));
    requestConsolidator.send(validReqMsg, this);
  }

  @Override
  public void hookOnNext(@NonNull T value) {
//    log.debug("in TestSubscriber.hookOnNext()");
    subscription.request(Long.MAX_VALUE);
//    log.debug("\n\n000000000000000000000000000000");
//    log.debug("000000000000000000000000000000");
//    log.debug(value.getClass().getSimpleName() + "\n\n");
    filterEventMessage(value).ifPresent(this::createReputationEventWithScore);
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

    eventMessageService.processIncoming(
        new EventMessage(textNoteEvent),
        subscriptionId);
  }

  @Override
  public void hookOnSubscribe(@NonNull Subscription subscription) {
//    log.debug("in TestSubscriber.hookOnSubscribe()");
    this.subscription = subscription;
    subscription.request(Long.MAX_VALUE);
  }

  public void addRelay(String name, String uri) {
    requestConsolidator.addRelay(name, uri);
  }

  public void removeRelay(String name) {
    requestConsolidator.removeRelay(name);
  }

  private Optional<EventMessage> filterEventMessage(T returnedBaseMessage) {
    return Optional.of(returnedBaseMessage)
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast);
  }

  private PublicKey getPublicKey(EventMessage eventMessage) {
    return ((GenericEvent) eventMessage.getEvent()).getPubKey();
  }
}
