package com.prosilion.afterimage.config;

import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

@Testcontainers
@EmbeddedRedisStandalone
@Slf4j
public class RxRCrossIdeTestConfig extends ContainerTestConfig {
  @Bean
  @ServiceConnection
  public ComposeContainer composeContainerLocalDev() {
    return new ComposeContainer(
       new File("src/test/resources/docker-compose-local_ws.yml"))
       .waitingFor(AFTERIMAGE_DB, Wait.forHealthcheck())
       .withRemoveVolumes(true);
  }
}
