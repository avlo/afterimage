package com.prosilion.afterimage.config;

import com.prosilion.afterimage.client.SuperconductorRequestConsolidator;
import com.prosilion.subdivisions.client.reactive.ReactiveRequestConsolidator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:superconductor-relays.properties")
public class SuperconductorRelaysConfig {

  @Bean
  public Map<String, String> superconductorRelays() {
    ResourceBundle relaysBundle = ResourceBundle.getBundle("superconductor-relays");
    return relaysBundle.keySet().stream()
        .collect(Collectors.toMap(key -> key, relaysBundle::getString));
  }

  @Bean
  public ReactiveRequestConsolidator reactiveRequestConsolidator(Map<String, String> superconductorRelays) {
    return new ReactiveRequestConsolidator(superconductorRelays);
  }

  @Bean
  public SuperconductorRequestConsolidator superconductorRequestConsolidator(
      @NonNull ReactiveRequestConsolidator reactiveRequestConsolidator) {
    return new SuperconductorRequestConsolidator(reactiveRequestConsolidator);
  }
}

