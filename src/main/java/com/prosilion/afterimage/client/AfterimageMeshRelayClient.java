package com.prosilion.afterimage.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.subdivisions.client.reactive.ReactiveNostrRelayClient;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseMessage;
import nostr.event.message.EventMessage;
import nostr.event.message.OkMessage;
import nostr.event.message.ReqMessage;
import org.reactivestreams.Subscriber;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;

@Slf4j
public class AfterimageMeshRelayClient {
  private final ReactiveNostrRelayClient nostrRelayClient;

  public AfterimageMeshRelayClient(@NonNull String relayUri) {
    log.debug("relayUri: \n{}", relayUri);
    this.nostrRelayClient = new ReactiveNostrRelayClient(relayUri);
    System.out.println("relayUri: " + relayUri);
  }

  public AfterimageMeshRelayClient(@NonNull String relayUri, @NonNull SslBundles sslBundles) throws ExecutionException, InterruptedException {
    log.debug("relayUri: \n{}", relayUri);
    log.debug("sslBundles: \n{}", sslBundles);
    final SslBundle server = sslBundles.getBundle("server");
    log.debug("sslBundles name: \n{}", server);
    log.debug("sslBundles key: \n{}", server.getKey());
    log.debug("sslBundles protocol: \n{}", server.getProtocol());
    this.nostrRelayClient = new ReactiveNostrRelayClient(relayUri, sslBundles);
  }

  public void send(@NonNull EventMessage eventMessage, @NonNull Subscriber<OkMessage> subscriber) throws IOException {
    nostrRelayClient.send(eventMessage, subscriber);
  }

  public void send(@NonNull ReqMessage reqMessage, @NonNull Subscriber<BaseMessage> subscriber) throws JsonProcessingException {
    nostrRelayClient.send(reqMessage, subscriber);
  }
}
