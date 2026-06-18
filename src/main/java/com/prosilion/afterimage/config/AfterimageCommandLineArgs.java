package com.prosilion.afterimage.config;

import com.prosilion.nostr.util.Util;
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
    Util.debug(log, "entering AfterimageCommandLineArgs", "", true, '1');
    this.args = args;
    Util.debug(log, "exiting AfterimageCommandLineArgs", "", true, '2');
  }

  @Bean
  public String afterimageRelayUrl(@Value("${afterimage.relay.url:}") String afterimageRelayUrl) {
    Util.debug(log, "entering afterimageRelayUrl [{}]", afterimageRelayUrl, true, '3');
    String url = Arrays.stream(args.getSourceArgs())
        .filter(s -> s.contains("afterimage.relay.url"))
        .findFirst()
        .map(s -> Arrays.stream(s.split("="))
            .toList().get(1))
        .orElse(afterimageRelayUrl);

    Util.debug(log, "exiting afterimageRelayUrl: [{}]", url, true, '4');
    return url;
  }
}
