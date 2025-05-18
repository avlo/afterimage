package com.prosilion.afterimage.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseMessage;
import nostr.event.message.ReqMessage;
import org.reactivestreams.Subscriber;

@Slf4j
public class SuperconductorMeshService {
  private final ReactiveRequestConsolidator reactiveRequestConsolidator;

  public SuperconductorMeshService(@NonNull ReactiveRequestConsolidator reactiveRequestConsolidator) {
    this.reactiveRequestConsolidator = reactiveRequestConsolidator;
  }

  public void send(@NonNull ReqMessage reqMessage, @NonNull Subscriber<BaseMessage> subscriber) throws JsonProcessingException {
    reactiveRequestConsolidator.send(reqMessage, subscriber);
  }

  public void addRelay(String name, String uri) {
    reactiveRequestConsolidator.addRelay(name, uri);
  }

  public void removeRelay(String name) {
    reactiveRequestConsolidator.removeRelay(name);
  }
}
