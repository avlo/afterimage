package com.prosilion.afterimage.config;

import com.prosilion.afterimage.util.AfterimageRelayClient;
import java.util.concurrent.ExecutionException;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

@Lazy
@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "false",
    matchIfMissing = true)
//@ComponentScan(basePackages = {"com.prosilion.superconductor.*"})
//@EnableJpaRepositories("com.prosilion.superconductor.repository")
public class AfterimageRelayMeshWsConfig {

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public AfterimageRelayClient afterimageRelayClient(@NonNull @Value("${afterimage.relay.url}") String relayUri) throws ExecutionException, InterruptedException {
    return new AfterimageRelayClient(relayUri);
  }
}
