package com.prosilion.afterimage.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.subdivisions.client.reactive.ReactiveNostrRelayClient;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.impl.GenericEvent;
import nostr.event.message.EventMessage;
import nostr.event.message.OkMessage;
import nostr.event.message.ReqMessage;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import reactor.core.publisher.Flux;

@Slf4j
public class AfterimageRelayReactiveClient {
  private final ReactiveNostrRelayClient nostrRelayClient;

  public AfterimageRelayReactiveClient(@NonNull String relayUri) {
    log.debug("relayUri: \n{}", relayUri);
    this.nostrRelayClient = new ReactiveNostrRelayClient(relayUri);
    System.out.println("relayUri: " + relayUri);
  }

  public AfterimageRelayReactiveClient(@NonNull String relayUri, @NonNull SslBundles sslBundles) throws ExecutionException, InterruptedException {
    log.debug("relayUri: \n{}", relayUri);
    log.debug("sslBundles: \n{}", sslBundles);
    final SslBundle server = sslBundles.getBundle("server");
    log.debug("sslBundles name: \n{}", server);
    log.debug("sslBundles key: \n{}", server.getKey());
    log.debug("sslBundles protocol: \n{}", server.getProtocol());
    this.nostrRelayClient = new ReactiveNostrRelayClient(relayUri, sslBundles);
  }

  public OkMessage send(@NonNull EventMessage eventMessage) throws IOException {
    Flux<OkMessage> send = nostrRelayClient.send(eventMessage);
    OkMessage okMessage = send.blockFirst();
    return okMessage;
  }

  public List<GenericEvent> send(@NonNull ReqMessage reqMessage) throws JsonProcessingException {
    Flux<GenericEvent> send = nostrRelayClient.send(reqMessage);
    List<GenericEvent> block = send.collectList().block();
    return block;
  }
}
