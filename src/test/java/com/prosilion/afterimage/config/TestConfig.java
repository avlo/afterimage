package com.prosilion.afterimage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class TestConfig {

  @Bean
  String afterimageRelayUrl(@NonNull @Value("${afterimage.relay.url}") String relayUrl) {
    return relayUrl;
  }
}
