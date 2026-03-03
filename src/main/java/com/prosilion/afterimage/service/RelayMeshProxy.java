package com.prosilion.afterimage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import com.prosilion.superconductor.base.util.RequestSubscriber;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class RelayMeshProxy extends RequestSubscriber<BaseMessage> {
  private final ReactiveRequestConsolidator relayRequestConsolidator;
  private final EventKindPluginIF eventKindPluginIF;

  public RelayMeshProxy(@NonNull EventKindPluginIF eventKindPluginIF) {
    this.relayRequestConsolidator = new ReactiveRequestConsolidator();
    this.eventKindPluginIF = eventKindPluginIF;
  }

  public void setUpRequestFlux(@NonNull Filters filters, @NonNull List<String> relayUrl) throws JsonProcessingException {
    for (String relay : relayUrl) {
      setUpRequestFlux(filters, relay);
    }
  }

  public void setUpRequestFlux(@NonNull Filters filters, @NonNull String relayUrl) throws JsonProcessingException {
    log.debug("setUpRequestFlux called with filters:\n[{}]",
        filters.toString(2));

    relayRequestConsolidator.send(
        new ReqMessage(generateRandomHex64String(), filters),
        this,
        relayUrl);

    this.getItems().forEach(item ->
        filterEventMessageEvent(item).ifPresent(this::processIncoming));
  }

  private void processIncoming(EventIF eventIF) {
    log.debug("**** RelayMeshProxy **** callback retrieved incoming...:\n  Kind[{}]: {}\ncontent:\n{}",
        eventIF.getKind().getValue(),
        eventIF.getKind().getName().toUpperCase(),
        eventIF.createPrettyPrintJson());

    eventKindPluginIF.processIncomingEvent(eventIF);
  }

  private Optional<EventIF> filterEventMessageEvent(BaseMessage returnedBaseMessage) {
    return Optional.of(returnedBaseMessage)
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent);
  }

  public static String generateRandomHex64String() {
    return UUID.randomUUID().toString().concat(UUID.randomUUID().toString()).replaceAll("[^A-Za-z0-9]", "");
  }
}
