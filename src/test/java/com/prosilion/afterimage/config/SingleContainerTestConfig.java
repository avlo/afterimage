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
public class SingleContainerTestConfig {
  @Bean
  @ServiceConnection
  public ComposeContainer composeSingleContainerLocalDev() {
    return new ComposeContainer(
        new File("src/test/resources/docker-compose-local_ws.yml"))
        .waitingFor("afterimage-db", Wait.forHealthcheck())
        .withRemoveVolumes(true);
  }

  @Bean
  @ServiceConnection
  public ComposeContainer composeSingleContainerSuperconductorDocker() {
    return new ComposeContainer(
        new File("src/test/resources/afterimage-docker-compose-single-sc-local-dev/afterimage-docker-compose-dev-test-ws.yml"))
        .waitingFor("superconductor-afterimage", Wait.defaultWaitStrategy())
        .withRemoveVolumes(true);
  }
}
