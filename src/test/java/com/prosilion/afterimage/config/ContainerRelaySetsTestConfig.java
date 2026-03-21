package com.prosilion.afterimage.config;

import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@EmbeddedRedisStandalone
@Slf4j
public class ContainerRelaySetsTestConfig {
  public static final String SUPERCONDUCTOR_AFTERIMAGE = "superconductor-afterimage";
  public static final String AFTERIMAGE_APP_TWO = "afterimage-app-two";

//  @Bean
//  public Network relaySetsNetwork() {
//    return Network.newNetwork();
//  }
  
  @Bean
//  @ServiceConnection
  public ComposeContainer composeRelaySetsContainerLocalDev() {
    return new ComposeContainer(
        new File("src/test/resources/docker-compose-local_ws.yml"))
        .waitingFor("afterimage-db", Wait.forHealthcheck())
        .withRemoveVolumes(true);
  }

  @Bean
  @ServiceConnection
  public ComposeContainer composeRelaySetsSuperconductorContainerDocker() {
    return new ComposeContainer(
        new File("src/test/resources/relay-sets/docker-compose-superconductor/docker-compose-superconductor.yml"))
        .waitingFor(SUPERCONDUCTOR_AFTERIMAGE, Wait.defaultWaitStrategy())
        .withExposedService(SUPERCONDUCTOR_AFTERIMAGE, 5555)
        .withRemoveVolumes(true);
  }

  @Bean
  public String superconductorDockerRelayUrl(ComposeContainer composeRelaySetsSuperconductorContainerDocker) {
    String serviceHost = composeRelaySetsSuperconductorContainerDocker
        .getServiceHost(SUPERCONDUCTOR_AFTERIMAGE, 5555);
    log.debug("SUPERCONDUCTOR_AFTERIMAGE serviceHost: {}", serviceHost);

    Integer servicePort = composeRelaySetsSuperconductorContainerDocker
        .getServicePort(SUPERCONDUCTOR_AFTERIMAGE, 5555);
    log.debug("SUPERCONDUCTOR_AFTERIMAGE servicePort: {}", serviceHost);

    String url = "ws://" + serviceHost + ":" + servicePort;
    log.debug("constructed superconductorRelayUrl: {}", url);
    return url;
  }

  @Bean
  @ServiceConnection
  public ComposeContainer composeRelaySetsAfterimageTwoContainerDocker() {
    return new ComposeContainer(
        new File("src/test/resources/relay-sets/docker-compose-afterimage-two/docker-compose-afterimage-two.yml"))
        .waitingFor(AFTERIMAGE_APP_TWO, Wait.defaultWaitStrategy())
        .withExposedService(AFTERIMAGE_APP_TWO, 5556)
        .withRemoveVolumes(true);
  }

  @Bean
  public String afterimageDockerRelayUrl(ComposeContainer composeRelaySetsAfterimageTwoContainerDocker) {
    String serviceHost = composeRelaySetsAfterimageTwoContainerDocker
        .getServiceHost(AFTERIMAGE_APP_TWO, 5556);
    log.debug("AFTERIMAGE_APP_TWO serviceHost: {}", serviceHost);

    Integer servicePort = composeRelaySetsAfterimageTwoContainerDocker
        .getServicePort(AFTERIMAGE_APP_TWO, 5556);
    log.debug("AFTERIMAGE_APP_TWO servicePort: {}", serviceHost);

    String url = "ws://" + serviceHost + ":" + servicePort;
    log.debug("constructed afterimageRelayUrlTwo: {}", url);
    return url;
  }
}
