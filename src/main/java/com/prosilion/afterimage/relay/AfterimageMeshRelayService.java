package com.prosilion.afterimage.relay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.subdivisions.client.reactive.ReactiveNostrRelayClient;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.lang.NonNull;

@Slf4j
// TODO: below class currently only used by IT's, consider replace w/ relevant alt
public class AfterimageMeshRelayService {
  private final ReactiveNostrRelayClient nostrRelayClient;

  public AfterimageMeshRelayService(@NonNull String relayUri) {
    log.debug("relayUri: \n{}", relayUri);
    this.nostrRelayClient = new ReactiveNostrRelayClient(relayUri);
    System.out.println("relayUri: " + relayUri);
  }

  public AfterimageMeshRelayService(@NonNull String relayUri, @NonNull SslBundles sslBundles) throws ExecutionException, InterruptedException {
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

  public void send(@NonNull ReqMessage reqMessage, @NonNull Subscriber<BaseMessage> subscriber) throws JsonProcessingException, NostrException {
    nostrRelayClient.send(reqMessage, subscriber);
  }
}
