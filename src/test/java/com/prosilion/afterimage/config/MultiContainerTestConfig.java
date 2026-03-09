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
public class MultiContainerTestConfig {
  @Bean
//  @RestartScope
  @ServiceConnection
  public ComposeContainer composeContainerLocalDev() {
    return new ComposeContainer(
        new File("src/test/resources/docker-compose-local_ws.yml"))
        .waitingFor("afterimage-db", Wait.forHealthcheck())
        .withRemoveVolumes(true);
  }

  @Bean
//  @RestartScope
  @ServiceConnection
  public ComposeContainer composeContainerDocker() {
    return new ComposeContainer(
        new File("src/test/resources/afterimage-docker-compose-multi-scs-and-aimgs-local-dev/afterimage-docker-compose-dev-test-ws.yml"))
// original Wait.forHealthcheck() calls do not work due to wget unavailable in container
        .waitingFor("afterimage-app", Wait.defaultWaitStrategy())
        .waitingFor("superconductor-afterimage", Wait.defaultWaitStrategy())
//        .waitingFor("superconductor-afterimage-two", Wait.defaultWaitStrategy())
        .withRemoveVolumes(true);
  }
}
