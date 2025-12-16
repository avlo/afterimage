package com.prosilion.afterimage.config;

import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import java.io.File;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@EmbeddedRedisStandalone
public class TestcontainersConfig {
  @Bean
  @ServiceConnection
  public ComposeContainer composeContainerLocalDev() {
    return new ComposeContainer(
        new File("src/test/resources/docker-compose-local_ws.yml"))
        .waitingFor("afterimage-db", Wait.forHealthcheck())
        .withRemoveVolumes(true);
  }

  @Bean
  @ServiceConnection
  public ComposeContainer composeContainerDocker() {
    return new ComposeContainer(
        new File("src/test/resources/afterimage-docker-compose-local-dev/afterimage-docker-compose-dev-test-ws.yml"))
        .waitingFor("afterimage-app", Wait.forHealthcheck())
        .waitingFor("superconductor-afterimage", Wait.forHealthcheck())
        .waitingFor("superconductor-afterimage-two", Wait.forHealthcheck())
        .withRemoveVolumes(true);
  }
}
