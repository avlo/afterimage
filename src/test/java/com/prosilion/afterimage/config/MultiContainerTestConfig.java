package com.prosilion.afterimage.config;

import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@EmbeddedRedisStandalone
@Slf4j
public class MultiContainerTestConfig extends ContainerTestConfig {
  @Bean
  @ServiceConnection
  public ComposeContainer composeContainerLocalDev() {
    return new ComposeContainer(
       new File("src/test/resources/docker-compose-local_ws.yml"))
       .waitingFor(AFTERIMAGE_DB, Wait.forHealthcheck())
       .withRemoveVolumes(true);
  }

  @Bean
  @ServiceConnection
  public ComposeContainer composeContainerDocker() {
    return new ComposeContainer(
       new File("src/test/resources/afterimage-docker-compose-multi-scs-and-aimgs-local-dev/afterimage-docker-compose-dev-test-ws.yml"))
       .waitingFor(SUPERCONDUCTOR_AFTERIMAGE, Wait.forLogMessage(".*Started " + SUPERCONDUCTOR_REDIS_APPLICATION + ".*\\n", 1))
       .withExposedService(SUPERCONDUCTOR_AFTERIMAGE, 5555)

       .waitingFor(AFTERIMAGE_APP_TWO, Wait.forLogMessage(".*Started " + AFTERIMAGE_REDIS_APPLICATION + ".*\\n", 1))
       .withExposedService(AFTERIMAGE_APP_TWO, 5556)

       .waitingFor(AFTERIMAGE_APP_THREE, Wait.forLogMessage(".*Started " + AFTERIMAGE_REDIS_APPLICATION + ".*\\n", 1))
       .withExposedService(AFTERIMAGE_APP_THREE, 5556)

       .withRemoveVolumes(true);
  }
}
