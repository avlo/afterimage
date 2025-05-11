package com.prosilion.afterimage.config;

import com.prosilion.afterimage.util.AfterimageRelayReactiveClient;
import com.prosilion.afterimage.util.AfterimageRelayStandardClient;
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
  public AfterimageRelayStandardClient afterimageRelayClient(@NonNull @Value("${afterimage.relay.url}") String relayUri) throws ExecutionException, InterruptedException {
    return new AfterimageRelayStandardClient(relayUri);
  }

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public AfterimageRelayReactiveClient afterimageReactiveRelayClient(@NonNull @Value("${afterimage.relay.url}") String relayUri) {
    return new AfterimageRelayReactiveClient(relayUri);
  }
}
