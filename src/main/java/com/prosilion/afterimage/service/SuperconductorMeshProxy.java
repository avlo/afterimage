package com.prosilion.afterimage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import com.prosilion.superconductor.service.message.event.EventMessageServiceIF;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseMessage;
import nostr.event.filter.Filters;
import nostr.event.filter.VoteTagFilter;
import nostr.event.message.EventMessage;
import nostr.event.message.ReqMessage;
import nostr.event.tag.VoteTag;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.BaseSubscriber;

@Slf4j
@Component
public class SuperconductorMeshProxy<T extends BaseMessage> extends BaseSubscriber<T> {
  private final ReactiveRequestConsolidator superconductorRequestConsolidator;
  private final EventMessageServiceIF<EventMessage> afterimageEventMessageService;

  private Subscription subscription;

  private final String subscriptionId = Factory.generateRandomHex64String();
  private final VoteTag voteTag = new VoteTag(1);

  @Autowired
  public SuperconductorMeshProxy(
      @NonNull EventMessageServiceIF<EventMessage> eventMessageService,
      @NonNull Map<String, String> superconductorRelays) throws JsonProcessingException {
    this.afterimageEventMessageService = eventMessageService;
    this.superconductorRequestConsolidator = new ReactiveRequestConsolidator(superconductorRelays);
    setUpReputationReqFlux();
  }

  private void setUpReputationReqFlux() throws JsonProcessingException {
    ReqMessage voteReqMsg = new ReqMessage(subscriptionId,
        new Filters(
            new VoteTagFilter<>(voteTag)));
    superconductorRequestConsolidator.send(voteReqMsg, this);
  }

  @Override
  public void hookOnNext(@NonNull T value) {
//    log.debug("in TestSubscriber.hookOnNext()");
    subscription.request(Long.MAX_VALUE);
//    log.debug("\n\n000000000000000000000000000000");
//    log.debug("000000000000000000000000000000");
//    log.debug(value.getClass().getSimpleName() + "\n\n");
    filterEventMessage(value).ifPresent(this::processIncoming);
  }

  private void processIncoming(EventMessage eventMessage) {
//    log.debug("SuperconductorMeshProxy EventMessage content: {}", eventMessage);
    superconductorRequestConsolidator.getRelayNames().forEach(relayNameAsSessionId ->
        afterimageEventMessageService.processIncoming(eventMessage, relayNameAsSessionId));
  }

  private Optional<EventMessage> filterEventMessage(T returnedBaseMessage) {
    return Optional.of(returnedBaseMessage)
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast);
  }

  @Override
  public void hookOnSubscribe(@NonNull Subscription subscription) {
//    log.debug("in TestSubscriber.hookOnSubscribe()");
    this.subscription = subscription;
    subscription.request(Long.MAX_VALUE);
  }

  public void addRelay(@NonNull String name, @NonNull String uri) {
    superconductorRequestConsolidator.addRelay(name, uri);
  }

  public void removeRelay(@NonNull String name) {
    superconductorRequestConsolidator.removeRelay(name);
  }
}
