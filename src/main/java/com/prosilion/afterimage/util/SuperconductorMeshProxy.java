package com.prosilion.afterimage.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import com.prosilion.superconductor.service.event.type.EventTypePlugin;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseMessage;
import nostr.event.Kind;
import nostr.event.filter.Filters;
import nostr.event.filter.KindFilter;
import nostr.event.impl.GenericEvent;
import nostr.event.message.EventMessage;
import nostr.event.message.ReqMessage;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

@Slf4j
public class SuperconductorMeshProxy<T extends BaseMessage, U extends GenericEvent> extends BaseSubscriber<T> {
  private final ReactiveRequestConsolidator superconductorRequestConsolidator;
  private final EventTypePlugin<U> eventTypePlugin;

  private Subscription subscription;

  private final String subscriptionId = generateRandomHex64String();
  private final Kind voteKind = Kind.VOTE;

  public SuperconductorMeshProxy(
      @NonNull Map<String, String> superconductorRelays,
      @NonNull EventTypePlugin<U> eventTypePlugin) {
    this.superconductorRequestConsolidator = new ReactiveRequestConsolidator();
    this.eventTypePlugin = eventTypePlugin;
    addRelay(superconductorRelays);
  }

  public SuperconductorMeshProxy(
      @NonNull String relayName,
      @NonNull String relayUrl,
      @NonNull EventTypePlugin<U> eventTypePlugin) {
    this.superconductorRequestConsolidator = new ReactiveRequestConsolidator();
    this.eventTypePlugin = eventTypePlugin;
    addRelay(relayName, relayUrl);
  }

  public void addRelay(@NonNull Map<String, String> relays) {
    relays.forEach(superconductorRequestConsolidator::addRelay);
  }

  public void setUpReputationReqFlux() throws JsonProcessingException {
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

  @SneakyThrows
  private void processIncoming(EventMessage eventMessage) {
//    log.debug("SuperconductorMeshProxy EventMessage content: {}", eventMessage);
    for (String unused : superconductorRequestConsolidator.getRelayNames()) {
      eventTypePlugin.processIncomingEvent(
          (U) eventMessage.getEvent());
    }
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
