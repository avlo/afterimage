package com.prosilion.afterimage.util.config;

import com.prosilion.afterimage.util.NostrRelayService;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Lazy
@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "false")
public class NostrWsConfig {

  @Bean
//  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public NostrRelayService nostrRelayService(@Value("${afterimage.relay.uri}") String relayUri) throws ExecutionException, InterruptedException {
    return new NostrRelayService(relayUri);
  }
}
