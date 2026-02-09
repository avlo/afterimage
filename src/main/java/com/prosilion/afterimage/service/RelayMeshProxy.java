package com.prosilion.afterimage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import com.prosilion.superconductor.base.service.event.plugin.EventPluginIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventMaterializer;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.lang.NonNull;
import reactor.core.publisher.BaseSubscriber;

@Slf4j
public class RelayMeshProxy extends BaseSubscriber<BaseMessage> {
  private final ReactiveRequestConsolidator relayRequestConsolidator;
  private final EventPluginIF eventPlugin;
  private final EventMaterializer eventMaterializer;

  private Subscription subscription;
  private final String subscriptionId = generateRandomHex64String();

  public RelayMeshProxy(
      @NonNull Map<String, String> relaysNameUrlMap,
      @NonNull EventPluginIF eventPlugin,
      @NonNull EventMaterializer eventMaterializer) {
    this.relayRequestConsolidator = new ReactiveRequestConsolidator();
    this.eventPlugin = eventPlugin;
    this.eventMaterializer = eventMaterializer;
    addRelay(relaysNameUrlMap);
    log.debug("RelayMeshProxy ctor() connecting to relays: [{}]", relaysNameUrlMap);
  }

  public RelayMeshProxy(
      @NonNull String relayName,
      @NonNull String relayUrl,
      @NonNull EventPluginIF eventPlugin,
      @NonNull EventMaterializer eventMaterializer) {
    this.relayRequestConsolidator = new ReactiveRequestConsolidator();
    this.eventPlugin = eventPlugin;
    this.eventMaterializer = eventMaterializer;
    addRelay(relayName, relayUrl);
  }

  public void addRelay(@NonNull Map<String, String> relays) {
    BiConsumer<String, String> addRelay = relayRequestConsolidator::addRelay;
    relays.forEach(addRelay);
  }

  public void setUpRequestFlux(Filters filters) throws JsonProcessingException, NostrException {
    relayRequestConsolidator.send(
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
    filterEventMessageEvent(value).ifPresent(this::processIncoming);
  }

  private void processIncoming(EventIF eventIF) {
    BaseEvent materialized = eventMaterializer.materialize(eventIF);
    relayRequestConsolidator.getRelayNames()
        .forEach(s ->
            eventPlugin.processIncomingEvent(materialized));
  }

  private Optional<EventIF> filterEventMessageEvent(BaseMessage returnedBaseMessage) {
    return Optional.of(returnedBaseMessage)
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent);
  }

  @Override
  public void hookOnSubscribe(@NonNull Subscription subscription) {
//    log.debug("in TestSubscriber.hookOnSubscribe()");
    subscription.request(Long.MAX_VALUE);
    this.subscription = subscription;
  }

  public void addRelay(@NonNull String name, @NonNull String uri) {
    relayRequestConsolidator.addRelay(name, uri);
  }

  public void removeRelay(@NonNull String name) {
    relayRequestConsolidator.removeRelay(name);
  }

  public static String generateRandomHex64String() {
    return UUID.randomUUID().toString().concat(UUID.randomUUID().toString()).replaceAll("[^A-Za-z0-9]", "");
  }
}
