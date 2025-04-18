package com.prosilion.afterimage.config;

import com.prosilion.afterimage.util.NostrRelayService;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "false")
@ComponentScan(basePackages = {"com.prosilion.superconductor.*"})
@EnableJpaRepositories("com.prosilion.superconductor.repository")
public class NostrWsConfig {

  public NostrWsConfig() {
    System.out.println("NostrWsConfig()");
  }

  @Lazy
  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public NostrRelayService nostrRelayService(@Value("${superconductor.relay.url}") String relayUri) throws ExecutionException, InterruptedException {
    return new NostrRelayService(relayUri);
  }
}
