package com.prosilion.afterimage.service.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.util.Util;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.subdivisions.client.RequestSubscriberDelegate;
import com.prosilion.subdivisions.client.reactive.MultiRelaySubscriptionsManager;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;

@Slf4j
public class RelayMeshProxy implements RelayMeshProxyIF {
  @Getter
  private final Duration timeout = RequestSubscriber.DEFAULT_TIMEOUT_3000_MS;

  private final EventKindPluginIF eventKindPluginIF;
  private final Map<Subscription, Relay> subscriptionRelayMap = new HashMap<>();

  public RelayMeshProxy(@NonNull EventKindPluginIF eventKindPluginIF) {
    this.eventKindPluginIF = eventKindPluginIF;
  }

  @Override
  public void activateRequestFlux(@NonNull Filters filters, @NonNull Set<Relay> relays) {
    for (Relay relay : relays) {
      activateRequestFlux(filters, relay);
    }
  }

  @Override
  public void activateRequestFlux(@NonNull Filters filters, @NonNull Relay relay) {
    log.debug("activateRequestFlux() called with filters:\n  [{}]\nrelay: [{}]", filters.toString(2), relay);

    String subscriptionId = Util.generateRandomHex64String();
    log.debug("calling new MultiRelaySubscriptionsManager().send(...) with subscriptionId: [{}]", subscriptionId);
    try {
      RequestSubscriberDelegate<BaseMessage> subscriberDelegate = new RequestSubscriberDelegate<>(this);
      new MultiRelaySubscriptionsManager()
         .send(
            new ReqMessage(
               subscriptionId,
               filters),
            relay.getUrl(),
            subscriberDelegate);
      subscriptionRelayMap.put(subscriberDelegate.getSubscription(), relay);
    } catch (JsonProcessingException e) {
      throw new NostrException("activateRequestFlux(...) multiRelaySubscriptionsManager.send(...) shit the bed", e);
    }
  }

  @Override
  public void doDelegate(@NonNull BaseMessage baseMessage, @NonNull Subscription subscription) {
    String encode;
    try {
      encode = baseMessage.encode();
    } catch (JsonProcessingException e) {
      throw new NostrException("doDelegate(...) baseMessage.encode() shit the bed", e);
    }
    log.debug("doDelegate(...) returned baseMessage:\n  {}", Util.prettyFormatJson(encode));
    Optional<EventIF> eventIF = filterEventMessageEvent(baseMessage);
    log.debug("filterEventMessageEvent(baseMessage) returned: \n{}",
       eventIF.map(EventIF::createPrettyPrintJson).orElse("  EMPTY Optional<EventIF>.  will not call processIncoming()"));

    Relay relay = subscriptionRelayMap.get(subscription);
    log.debug("subscriptionRelayMap.get(subscription):\n  [{}]\nreturned relay:\n  [{}]", subscription, relay);
    eventIF.ifPresent(event -> processIncoming(event, relay));
  }

  @Override
  public void dispose() {
  }

  private void processIncoming(EventIF eventIF, Relay relay) {
    log.debug("**** RelayMeshProxy **** callback retrieved incoming...:\n  Kind[{}]: {}\ncontent:\n{}",
       eventIF.getKind().getValue(),
       eventIF.getKind().getName().toUpperCase(),
       eventIF.createPrettyPrintJson());

    log.debug("calling eventKindPluginIF.processIncomingEvent(eventIF) using eventKindPluginIF type: [{}] ", eventKindPluginIF.getClass().getSimpleName());
    eventKindPluginIF.processIncomingEvent(eventIF, relay);
  }

  private Optional<EventIF> filterEventMessageEvent(BaseMessage returnedBaseMessage) {
    return Optional.of(returnedBaseMessage)
       .filter(EventMessage.class::isInstance)
       .map(EventMessage.class::cast)
       .map(EventMessage::getEvent);
  }
}
