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
public class MultiContainerSameRelayTestConfig {
  private final static String SUPERCONDUCTOR_AFTERIMAGE = "superconductor-afterimage";

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
        new File("src/test/resources/afterimage-docker-compose-same-relay-local-dev/afterimage-docker-compose-dev-test-ws.yml"))
// original Wait.forHealthcheck() calls do not work due to wget unavailable in container
        .waitingFor(SUPERCONDUCTOR_AFTERIMAGE, Wait.defaultWaitStrategy())
        .withExposedService(SUPERCONDUCTOR_AFTERIMAGE, 5555)
        .withRemoveVolumes(true);
  }

  @Bean
  public String superconductorDockerRelayUrl(ComposeContainer composeContainerDocker) {
    String serviceHost = composeContainerDocker.getServiceHost(SUPERCONDUCTOR_AFTERIMAGE, 5555);
    log.debug("SUPERCONDUCTOR_AFTERIMAGE serviceHost: {}", serviceHost);

    Integer servicePort = composeContainerDocker.getServicePort(SUPERCONDUCTOR_AFTERIMAGE, 5555);
    log.debug("SUPERCONDUCTOR_AFTERIMAGE servicePort: {}", serviceHost);

    String url = "ws://" + serviceHost + ":" + servicePort;
    log.debug("constructed superconductorRelayUrl: {}", url);
    return url;
  }
}
