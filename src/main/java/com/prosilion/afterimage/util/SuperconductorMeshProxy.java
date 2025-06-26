package com.prosilion.afterimage.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.GenericEventKindIF;
import com.prosilion.nostr.event.GenericEventKindTypeIF;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import com.prosilion.superconductor.service.event.service.plugin.EventKindPluginIF;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.lang.NonNull;
import reactor.core.publisher.BaseSubscriber;

@Slf4j
public class SuperconductorMeshProxy extends BaseSubscriber<BaseMessage> {
  private final ReactiveRequestConsolidator superconductorRequestConsolidator;
  private final EventKindPluginIF eventKindPlugin;

  private Subscription subscription;
  private final String subscriptionId = generateRandomHex64String();

  public SuperconductorMeshProxy(
      @NonNull Map<String, String> superconductorRelays,
      @NonNull EventKindPluginIF eventKindPlugin) {
    this.superconductorRequestConsolidator = new ReactiveRequestConsolidator();
    this.eventKindPlugin = eventKindPlugin;
    addRelay(superconductorRelays);
    log.debug("SuperconductorMeshProxy ctor() connecting to relays: [{}]", superconductorRelays);
  }

  public SuperconductorMeshProxy(
      @NonNull String relayName,
      @NonNull String relayUrl,
      @NonNull EventKindPluginIF eventKindPlugin) {
    this.superconductorRequestConsolidator = new ReactiveRequestConsolidator();
    this.eventKindPlugin = eventKindPlugin;
    addRelay(relayName, relayUrl);
  }

  public void addRelay(@NonNull Map<String, String> relays) {
    relays.forEach(superconductorRequestConsolidator::addRelay);
  }

  public void setUpReputationReqFlux(Filters filters) throws JsonProcessingException, NostrException {
    superconductorRequestConsolidator.send(
        new ReqMessage(subscriptionId, filters),
        this);
  }

  @Override
  public void hookOnNext(@NonNull BaseMessage value) {
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
//    TODO: resolve unused
    for (String unused : superconductorRequestConsolidator.getRelayNames()) {
      GenericEventKindIF event1 = eventMessage.getEvent();
      GenericEventKindTypeIF event = (GenericEventKindTypeIF) event1;
      eventKindPlugin.processIncomingEvent(
          event);
    }
  }

  private Optional<EventMessage> filterEventMessage(BaseMessage returnedBaseMessage) {
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
