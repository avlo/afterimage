package com.prosilion.afterimage.service;

import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Slf4j
@Testcontainers
@EmbeddedRedisStandalone
public abstract class DockerITComposeContainer {

  @Container
  public static final ComposeContainer DOCKER_COMPOSE_CONTAINER;

  static {
    DOCKER_COMPOSE_CONTAINER = new ComposeContainer(
        new File("src/test/resources/docker-compose/superconductor-docker-compose-dev-test-ws.yml"))
        .withExposedService("superconductor-afterimage", 5555)
        .withRemoveVolumes(true);
  }

  @BeforeAll
  static void beforeAll() {
    log.info("calling DOCKER_COMPOSE_CONTAINER.start()....");
    DOCKER_COMPOSE_CONTAINER.start();
    log.info("... done DOCKER_COMPOSE_CONTAINER.start()");
  }
}
