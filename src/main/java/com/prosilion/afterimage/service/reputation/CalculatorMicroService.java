package com.prosilion.afterimage.service.reputation;

import com.prosilion.afterimage.config.ScoreVoteEvents;
import com.prosilion.nostr.NostrException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
public class CalculatorMicroService implements CalculatorServiceIF {
  WebClient webClient;
  URI liveUri;
  //  http://localhost:5556/api/reputation
  private final String SCHEME = "http";
  private final String host = "localhost";
  private final String port = "5556";
  private final String tenant = "api";
  private final String uri = "reputation";

  public CalculatorMicroService(@NonNull URL url) throws URISyntaxException {
    this.webClient = WebClient.builder()
        .filter(
            logRequest())
        .build();
    this.liveUri = url.toURI();
  }

  @Override
  public BigDecimal calculate(@NonNull ScoreVoteEvents scoreVoteEvents) throws NostrException {

//    headersSpec.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).acceptCharset(StandardCharsets.UTF_8).ifNoneMatch("*").ifModifiedSince(ZonedDateTime.now()).retrieve();

//    ReputationServiceSubscriber<BigDecimal> subscriber = new ReputationServiceSubscriber<>();

    WebClient.RequestBodySpec uri1 = webClient
        .post()
        .uri(uriBuilder -> uriBuilder
            .scheme(SCHEME)
            .host(host)
            .port(port)
            .pathSegment(tenant)
            .path(uri)
            .build());

    WebClient.RequestHeadersSpec<?> body = uri1
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            BodyInserters.fromValue(scoreVoteEvents));

    BigDecimal block = body
        .retrieve()
        .bodyToMono(BigDecimal.class)
        .block();

//    List<BigDecimal> items1 = subscriber.getItems();
//    BigDecimal first = items1.getFirst();
    return block;
  }

  private ExchangeFilterFunction logRequest() {
    return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
      log.info(String.format("Request: %s %s", clientRequest.method(), clientRequest.url()));
      clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info(String.format("%s=%s", name, value))));
      return Mono.just(clientRequest);
    });
  }
}
