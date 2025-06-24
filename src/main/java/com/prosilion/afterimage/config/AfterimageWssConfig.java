package com.prosilion.afterimage.config;

import com.prosilion.afterimage.relay.AfterimageMeshRelayService;
import java.util.concurrent.ExecutionException;
import lombok.NonNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "true")
public class AfterimageWssConfig extends AfterimageBaseConfig {
  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public AfterimageMeshRelayService afterimageReactiveRelayClient(
      @NonNull String afterimageRelayUrl,
      @NonNull SslBundles sslBundles) throws ExecutionException, InterruptedException {
    return new AfterimageMeshRelayService(afterimageRelayUrl, sslBundles);
  }
}
