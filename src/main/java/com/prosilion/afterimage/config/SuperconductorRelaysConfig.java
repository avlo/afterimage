package com.prosilion.afterimage.config;

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
  public @NonNull Map<String, String> superconductorRelays() {
    ResourceBundle relaysBundle = ResourceBundle.getBundle("superconductor-relays");
    return relaysBundle.keySet().stream()
        .collect(Collectors.toMap(key -> key, relaysBundle::getString));
  }
}

