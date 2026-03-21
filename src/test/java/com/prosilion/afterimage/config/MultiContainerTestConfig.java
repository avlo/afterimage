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
public class MultiContainerTestConfig {
  private final static String SUPERCONDUCTOR_AFTERIMAGE = "superconductor-afterimage";
  public static final String AFTERIMAGE_APP_TWO = "afterimage-app-two";
  public static final String AFTERIMAGE_APP_THREE = "afterimage-app-three";

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
        .waitingFor(SUPERCONDUCTOR_AFTERIMAGE, Wait.defaultWaitStrategy())
        .withExposedService(SUPERCONDUCTOR_AFTERIMAGE, 5555)
        .waitingFor(AFTERIMAGE_APP_TWO, Wait.defaultWaitStrategy())
        .withExposedService(AFTERIMAGE_APP_TWO, 5556)
        .waitingFor(AFTERIMAGE_APP_THREE, Wait.defaultWaitStrategy())
        .withExposedService(AFTERIMAGE_APP_THREE, 5556)
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

  @Bean
  public String afterimageDockerRelayUrlTwo(ComposeContainer composeContainerDocker) {
    String serviceHost = composeContainerDocker.getServiceHost(AFTERIMAGE_APP_TWO, 5556);
    log.debug("AFTERIMAGE_APP_TWO serviceHost: {}", serviceHost);

    Integer servicePort = composeContainerDocker.getServicePort(AFTERIMAGE_APP_TWO, 5556);
    log.debug("AFTERIMAGE_APP_TWO servicePort: {}", serviceHost);

    String url = "ws://" + serviceHost + ":" + servicePort;
    log.debug("constructed afterimageRelayUrlTwo: {}", url);
    return url;
  }

  @Bean
  public String afterimageDockerRelayUrlThree(ComposeContainer composeContainerDocker) {
    String serviceHost = composeContainerDocker.getServiceHost(AFTERIMAGE_APP_THREE, 5556);
    log.debug("AFTERIMAGE_APP_THREE serviceHost: {}", serviceHost);

    Integer servicePort = composeContainerDocker.getServicePort(AFTERIMAGE_APP_THREE, 5556);
    log.debug("AFTERIMAGE_APP_THREE servicePort: {}", serviceHost);

    String url = "ws://" + serviceHost + ":" + servicePort;
    log.debug("constructed afterimageRelayUrlThree: {}", url);
    return url;
  }
}
