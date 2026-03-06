package com.prosilion.afterimage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.nostr.NostrException;
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
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.lang.NonNull;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.SignalType;
import reactor.util.context.Context;

@Slf4j
public class RelayMeshProxy extends BaseSubscriber<BaseMessage> {
  private final EventKindPluginIF eventKindPluginIF;
  private final ReactiveRequestConsolidator reactiveRequestConsolidator;
  private Subscription subscription;

  public RelayMeshProxy(
      @NonNull EventKindPluginIF eventKindPluginIF,
      @NonNull ReactiveRequestConsolidator reactiveRequestConsolidator) {
    this.eventKindPluginIF = eventKindPluginIF;
    this.reactiveRequestConsolidator = reactiveRequestConsolidator;
  }

  public void setUpRequestFlux(@NonNull Filters filters, @NonNull List<String> relayUrl) {
    for (String relay : relayUrl) {
      setUpRequestFlux(filters, relay);
    }
  }

  public void setUpRequestFlux(@NonNull Filters filters, @NonNull String relayUrl) {
    log.debug("setUpRequestFlux called with filters:\n[{}]",
        filters.toString(2));

    try {
      reactiveRequestConsolidator.send(
          new ReqMessage(generateRandomHex64String(), filters),
          this,
          relayUrl);
    } catch (JsonProcessingException e) {
      log.debug("XXXXXXXXXXXXXXXXXXXXX");
      log.debug("XXXXXXXXXXXXXXXXXXXXX");
      log.debug("Afterimage RelayMeshProxy encountered JsonProcessingException:\n", e);
      log.debug("XXXXXXXXXXXXXXXXXXXXX");
      log.debug("XXXXXXXXXXXXXXXXXXXXX");
      throw new NostrException("Afterimage RelayMeshProxy encountered JsonProcessingException: ", e);
    }
  }

  @Override
  public void hookOnSubscribe(@NonNull Subscription subscription) {
    log.debug("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    log.debug("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    subscription.request(Long.MAX_VALUE);
    this.subscription = subscription;
    log.debug("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    log.debug("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
  }

  @Override
  public void hookOnNext(@NonNull BaseMessage value) {
    log.debug("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");
    log.debug("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");
    subscription.request(Long.MAX_VALUE);
    Optional<EventIF> eventIF = filterEventMessageEvent(value);
    log.debug("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");
    log.debug("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");
    eventIF.ifPresent(this::processIncoming);
  }

  @Override
  public boolean isDisposed() {
    boolean disposed = super.isDisposed();
    log.debug("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
    log.debug("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
    log.debug("isDisposed()? {}", disposed);
    log.debug("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
    log.debug("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
    return disposed;
  }

  @Override
  protected void hookOnError(Throwable throwable) {
    log.debug("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
    log.debug("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
    log.debug("hookOnError()? {}", throwable.getMessage());
    log.debug("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
    log.debug("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
    super.hookOnError(throwable);
  }

  @Override
  protected void hookOnCancel() {
    log.debug("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC");
    log.debug("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC");
    log.debug("hookOnCancel()");
    log.debug("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC");
    log.debug("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC");
    super.hookOnCancel();
  }

  @Override
  protected void hookFinally(SignalType type) {
    log.debug("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
    log.debug("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
    log.debug("hookFinally() SignalType:  [{}]", type.name());
    log.debug("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
    log.debug("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
    super.hookFinally(type);
  }

  @Override
  public @NonNull Context currentContext() {
    log.debug("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    log.debug("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    Context context = super.currentContext();
    log.debug("currentContext() context:{}", context.stream().map(objectObjectEntry -> 
        String.format("  objectObjectEntry.getKey(): [%s]\n  objectObjectEntry.getVal(): [%s]\n", objectObjectEntry.getKey(), objectObjectEntry.getValue())));
    log.debug("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    log.debug("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    return context;
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

  public static String generateRandomHex64String() {
    return UUID.randomUUID().toString().concat(UUID.randomUUID().toString()).replaceAll("[^A-Za-z0-9]", "");
  }
}
