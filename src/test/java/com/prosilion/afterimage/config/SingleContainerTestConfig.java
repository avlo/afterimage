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
public class SingleContainerTestConfig extends ContainerTestConfig {

  @Bean
  @ServiceConnection
  public ComposeContainer composeSingleContainerLocalDev() {
    return new ComposeContainer(
       new File("src/test/resources/docker-compose-local_ws.yml"))
       .waitingFor(AFTERIMAGE_DB, Wait.forHealthcheck())
       .withRemoveVolumes(true);
  }

  @Bean
  @ServiceConnection
  public ComposeContainer composeSingleContainerSuperconductorDocker() {
    return new ComposeContainer(
       new File("src/test/resources/afterimage-docker-compose-single-sc-local-dev/afterimage-docker-compose-dev-test-ws.yml"))
       .waitingFor(SUPERCONDUCTOR_DB, Wait.forHealthcheck())
       .waitingFor(SUPERCONDUCTOR_AFTERIMAGE, Wait.forLogMessage(".*Started " + SUPERCONDUCTOR_REDIS_APPLICATION + ".*\\n", 1))
//       .withExposedService(SUPERCONDUCTOR_AFTERIMAGE, 5556)
       .withRemoveVolumes(true);
  }
}
