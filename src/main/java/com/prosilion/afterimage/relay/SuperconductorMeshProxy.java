package com.prosilion.afterimage.relay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import com.prosilion.superconductor.service.message.event.AutoConfigEventMessageServiceIF;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseMessage;
import nostr.event.Kind;
import nostr.event.filter.Filters;
import nostr.event.filter.KindFilter;
import nostr.event.message.EventMessage;
import nostr.event.message.ReqMessage;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.BaseSubscriber;

@Slf4j
@Component
public class SuperconductorMeshProxy<T extends BaseMessage> extends BaseSubscriber<T> {
  private final ReactiveRequestConsolidator superconductorRequestConsolidator;
  private final AutoConfigEventMessageServiceIF<EventMessage> eventMessageService;

  private Subscription subscription;

  private final String subscriptionId = generateRandomHex64String();
  private final Kind voteKind = Kind.VOTE;

  @Autowired
  public SuperconductorMeshProxy(
      @NonNull AutoConfigEventMessageServiceIF<EventMessage> eventMessageService,
      @NonNull Map<String, String> superconductorRelays) throws JsonProcessingException {
    this.eventMessageService = eventMessageService;
    this.superconductorRequestConsolidator = new ReactiveRequestConsolidator();
    addRelay(superconductorRelays);
  }

  public void addRelay(@NonNull Map<String, String> relays) throws JsonProcessingException {
    relays.forEach(superconductorRequestConsolidator::addRelay);
    setUpReputationReqFlux();
  }

  private void setUpReputationReqFlux() throws JsonProcessingException {
    superconductorRequestConsolidator.send(
        new ReqMessage(subscriptionId,
            new Filters(
                new KindFilter<>(voteKind))),
        this);
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
        eventMessageService.processIncoming(eventMessage, relayNameAsSessionId));
  }

  private Optional<EventMessage> filterEventMessage(T returnedBaseMessage) {
    return Optional.of(returnedBaseMessage)
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast);
  }

  @Override
  public void hookOnSubscribe(@NonNull Subscription subscription) {
//    log.debug("in TestSubscriber.hookOnSubscribe()");
    subscription.request(Long.MAX_VALUE);
    this.subscription = subscription;
  }

  public void addRelay(@NonNull String name, @NonNull String uri) {
    superconductorRequestConsolidator.addRelay(name, uri);
  }

  public void removeRelay(@NonNull String name) {
    superconductorRequestConsolidator.removeRelay(name);
  }

  public static String generateRandomHex64String() {
    return UUID.randomUUID().toString().concat(UUID.randomUUID().toString()).replaceAll("[^A-Za-z0-9]", "");
  }
}
