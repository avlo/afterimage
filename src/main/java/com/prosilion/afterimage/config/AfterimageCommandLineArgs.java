package com.prosilion.afterimage.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class AfterimageCommandLineArgs {
  private final ApplicationArguments args;

  @Autowired
  public AfterimageCommandLineArgs(ApplicationArguments args) {
    this.args = args;
  }

  @Bean
  @NotEmpty
  public String afterimageRelayUrl() {
    Optional.of(System.getProperty("server.port"))
        .orElseThrow(() ->
            new IllegalArgumentException("server.port parameter not found")
        );

    return Arrays.stream(args.getSourceArgs())
        .filter(s -> s.contains("afterimage.relay.url"))
        .findFirst()
        .map(s -> Arrays.stream(s.split("="))
            .toList().get(1))
        .orElseThrow(() ->
            new IllegalArgumentException("afterimage.relay.url parameter not found")
        );
  }
}
