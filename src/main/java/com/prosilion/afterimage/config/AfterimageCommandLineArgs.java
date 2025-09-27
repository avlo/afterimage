package com.prosilion.afterimage.config;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AfterimageCommandLineArgs {
  private final ApplicationArguments args;

  @Autowired
  public AfterimageCommandLineArgs(ApplicationArguments args) {
    this.args = args;
  }

  @Bean
  public String afterimageRelayUrl(@Value("${afterimage.relay.url:}") String afterimageRelayUrl) {
    String url = Arrays.stream(args.getSourceArgs())
        .filter(s -> s.contains("afterimage.relay.url"))
        .findFirst()
        .map(s -> Arrays.stream(s.split("="))
            .toList().get(1))
        .orElse(afterimageRelayUrl);

    log.debug("{} afterimageRelayUrl [{}]", getClass().getSimpleName(), url);
    return url;
  }
}
