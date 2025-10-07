package com.prosilion.afterimage.config;

import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import java.io.File;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@EmbeddedRedisStandalone
@TestConfiguration
public class LocalDevTestcontainersConfig {
  @Bean
  @ServiceConnection(name = "redis")
  public ComposeContainer composeContainer_1() {
    ComposeContainer composeContainer = new ComposeContainer(
        new File("src/test/resources/docker-compose-local_ws.yml"));
    return composeContainer;
  }

  public static final String SUPERCONDUCTOR_AFTERIMAGE = "superconductor-afterimage";
  public static final String AFTERIMAGE_APP = "afterimage-app";

  @Bean
  @ServiceConnection(name = "redis")
  public ComposeContainer composeContainer_2() {
    ComposeContainer composeContainer = new ComposeContainer(
        new File("src/test/resources/afterimage-docker-compose-local-dev/afterimage-docker-compose-dev-test-ws.yml"))
//        .withExposedService(SUPERCONDUCTOR_AFTERIMAGE, 5555)
//        .withExposedService(AFTERIMAGE_APP, 5557)
//        .withRemoveVolumes(true)
        ;
    return composeContainer;
  }
}
