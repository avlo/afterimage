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
  public ComposeContainer composeContainer() {
    return new ComposeContainer(
        new File("src/test/resources/docker-compose-local_ws.yml"));
  }
}
