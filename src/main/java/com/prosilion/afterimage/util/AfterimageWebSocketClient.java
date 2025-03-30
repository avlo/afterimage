package com.prosilion.afterimage.util;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseMessage;
import org.awaitility.Awaitility;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class AfterimageWebSocketClient extends TextWebSocketHandler {
  @Getter
  private final WebSocketSession clientSession;
  private final AtomicBoolean completed = new AtomicBoolean(false);

  @Getter
  private final List<String> events = Collections.synchronizedList(new ArrayList<>());

  public AfterimageWebSocketClient(@NonNull String relayUri) throws ExecutionException, InterruptedException {
    StandardWebSocketClient standardWebSocketClient = new StandardWebSocketClient();
    this.clientSession = getClientSession(relayUri, standardWebSocketClient);
    log.debug("Non-Secure (WS) WebSocket client connected {}", clientSession.getId());
  }

  public AfterimageWebSocketClient(@NonNull String relayUri, @NonNull SslBundles sslBundles) throws ExecutionException, InterruptedException {
    StandardWebSocketClient standardWebSocketClient = new StandardWebSocketClient();
    standardWebSocketClient.setSslContext(sslBundles.getBundle("server").createSslContext());
    this.clientSession = getClientSession(relayUri, standardWebSocketClient);
    log.debug("Secure (WSS) WebSocket client connected {}", clientSession.getId());
  }

  private WebSocketSession getClientSession(@NonNull String relayUri, StandardWebSocketClient standardWebSocketClient) throws InterruptedException, ExecutionException {
    return standardWebSocketClient
        .execute(
            this,
            new WebSocketHttpHeaders(),
            URI.create(relayUri))
        .get();
  }

  @Override
  protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) {
    events.add(message.getPayload());
    completed.setRelease(true);
  }

  public <T extends BaseMessage> void send(T eventMessage) throws IOException {
    send(eventMessage.encode());
  }

  public void send(String json) throws IOException {
    clientSession.sendMessage(new TextMessage(json));
    Awaitility.await()
        .timeout(66, TimeUnit.MINUTES)
        .untilTrue(completed);
    completed.setRelease(false);
  }
}
