package com.prosilion.afterimage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.subdivisions.client.RequestSubscriberDelegate;
import com.prosilion.subdivisions.client.reactive.MultiRelaySubscriptionsManager;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class RelayMeshProxy implements RelayMeshProxyIF {
  private final EventKindPluginIF eventKindPluginIF;

  public RelayMeshProxy(@NonNull EventKindPluginIF eventKindPluginIF) {
    this.eventKindPluginIF = eventKindPluginIF;
  }

  @Override
  public void activateRequestFlux(@NonNull Filters filters, @NonNull Set<String> relayUrl) {
    for (String relay : relayUrl) {
      activateRequestFlux(filters, relay);
    }
  }

  @Override
  public void activateRequestFlux(@NonNull Filters filters, @NonNull String relayUrl) {
    log.debug("activateRequestFlux() called with filters:\n  [{}]\nrelayUrl: [{}]",
        filters.toString(2),
        relayUrl);

    log.debug("calling new MultiRelaySubscriptionsManager().send(...)");
    try {
      new MultiRelaySubscriptionsManager()
          .send(
              new ReqMessage(
                  generateRandomHex64String(),
                  filters),
              relayUrl,
              new RequestSubscriberDelegate<>(this));
    } catch (JsonProcessingException e) {
      throw new NostrException("activateRequestFlux(...) multiRelaySubscriptionsManager.send(...) shit the bed", e);
    }
  }

  @SneakyThrows
  @Override
  public void doDelegate(@NonNull BaseMessage baseMessage) {
    log.debug("doDelegate(BaseMessage) returned baseMessage:\n  {}", baseMessage.encode());
    Optional<EventIF> eventIF = filterEventMessageEvent(baseMessage);
    log.debug("filterEventMessageEvent(baseMessage) returned: \n{}",
        eventIF.map(EventIF::createPrettyPrintJson).orElse("EMPTY Optional<EventIF>.  will not call processIncoming()"));
    eventIF.ifPresent(this::processIncoming);
  }

  private void processIncoming(EventIF eventIF) {
    log.debug("**** RelayMeshProxy **** callback retrieved incoming...:\n  Kind[{}]: {}\ncontent:\n{}",
        eventIF.getKind().getValue(),
        eventIF.getKind().getName().toUpperCase(),
        eventIF.createPrettyPrintJson());

    eventKindPluginIF.processIncomingEvent(eventIF);
  }

  private Optional<EventIF> filterEventMessageEvent(BaseMessage returnedBaseMessage) {
    Optional<EventIF> eventIF = Optional.of(returnedBaseMessage)
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent);
    return eventIF;
  }

  private static String generateRandomHex64String() {
    return UUID.randomUUID().toString().concat(UUID.randomUUID().toString()).replaceAll("[^A-Za-z0-9]", "");
  }
}
