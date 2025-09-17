package com.prosilion.afterimage.config;

import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.NonNull;

@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "true")
public class TestWssConfig {

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public AfterimageMeshRelayService afterimageReactiveRelayClient(
      @NonNull String afterimageRelayUrl,
      @NonNull SslBundles sslBundles) {
    return new AfterimageMeshRelayService(afterimageRelayUrl, sslBundles);
  }
}
