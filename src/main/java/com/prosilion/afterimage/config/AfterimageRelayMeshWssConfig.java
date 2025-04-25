package com.prosilion.afterimage.config;

import com.prosilion.afterimage.util.AfterimageRelayClient;
import java.util.concurrent.ExecutionException;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

@Lazy
@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "true")
//@ComponentScan(basePackages = {"com.prosilion.superconductor.*"})
//@EnableJpaRepositories("com.prosilion.superconductor.repository")
public class AfterimageRelayMeshWssConfig {

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public AfterimageRelayClient nostrRelayService(
      @NonNull @Value("${afterimage.relay.url}") String relayUri,
      @NonNull SslBundles sslBundles
  ) throws ExecutionException, InterruptedException {
    return new AfterimageRelayClient(relayUri, sslBundles);
  }
}
