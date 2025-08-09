package com.prosilion.afterimage.config;

import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.NonNull;

@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class TestWsConfig {

  @Bean
  String afterimageRelayUrl(@NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl) {
    return afterimageRelayUrl;
  }

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public AfterimageMeshRelayService afterimageMeshRelayService(@NonNull String afterimageRelayUrl) {
    return new AfterimageMeshRelayService(afterimageRelayUrl);
  }
}
