package com.prosilion.afterimage.service;

import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.lang.NonNull;
import reactor.core.publisher.BaseSubscriber;

@Slf4j
public class RelayMeshReactiveRequestConsolidatorProxy extends BaseSubscriber<BaseMessage> implements RelayMeshProxyIF {
  private Subscription subscription;

  private final EventKindPluginIF eventKindPluginIF;
  private final ReactiveRequestConsolidator reactiveRequestConsolidator;

  public RelayMeshReactiveRequestConsolidatorProxy(@NonNull EventKindPluginIF eventKindPluginIF) {
    this.eventKindPluginIF = eventKindPluginIF;
    this.reactiveRequestConsolidator = new ReactiveRequestConsolidator();
  }

  @Override
  public void hookOnSubscribe(@NonNull Subscription subscription) {
    this.subscription = subscription;
    subscription.request(1);
  }

  @Override
  public void hookOnNext(@NonNull BaseMessage value) {
    filterEventMessageEvent(value).ifPresent(this::processIncoming);
    subscription.request(1);
  }

  @Override
  public void activateRequestFlux(@NonNull Filters filters, @NonNull List<String> relayUrl) {
    for (String relay : relayUrl) {
      activateRequestFlux(filters, relay);
    }
  }

  @SneakyThrows
  @Override
  public void activateRequestFlux(@NonNull Filters filters, @NonNull String relayUrl) {
    log.debug("setUpRequestFlux called with filters:\n  [{}]",
        filters.toString(2));

    reactiveRequestConsolidator.send(
        new ReqMessage(generateRandomHex64String(), filters),
        this,
        relayUrl);
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
