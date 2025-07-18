package com.prosilion.afterimage.config;

import com.prosilion.afterimage.relay.AfterimageMeshRelayService;
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
public class AfterimageWsConfig extends AfterimageBaseConfig {
  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public AfterimageMeshRelayService afterimageMeshRelayService(@NonNull String afterimageRelayUrl) {
    return new AfterimageMeshRelayService(afterimageRelayUrl);
  }
}
