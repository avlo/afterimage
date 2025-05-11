package com.prosilion.afterimage.service;

import java.io.File;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

public abstract class CommonContainer {

  @Container
//  @ServiceConnection
  public static ComposeContainer superconductorContainer = new ComposeContainer(
      new File("src/test/resources/superconductor-docker-compose-dev_ws.yml"))
      .withExposedService("superconductor-afterimage", 5555, Wait.forHealthcheck());
}
