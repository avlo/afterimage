package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.config.AfterimageWsConfig;
import com.prosilion.afterimage.config.MultiContainerTestConfig;
import com.prosilion.superconductor.base.service.event.EventService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.assertj.ApplicationContextAssert;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(MultiContainerTestConfig.class)
public class AutoConfigurationIT {
  private final ApplicationContextRunner contextRunner;

  public AutoConfigurationIT() {
    ApplicationContextRunner acr = new ApplicationContextRunner()
//        .withConfiguration(
//            AutoConfigurations.of(EventKindServiceConfig.class)
//        )
        .withUserConfiguration(
//            AfterimageBaseConfig.class
            AfterimageWsConfig.class
        );
    this.contextRunner = acr;
  }

  @Test
  void defaultServiceBacksOff() {
    this.contextRunner
        .withUserConfiguration(
            AfterimageWsConfig.class)
        .run(context -> {
          ApplicationContextAssert<ConfigurableApplicationContext> anAssert = getAnAssert(context);
          AbstractObjectAssert<?, Object> sameAs = getSameAs(context);
          System.out.println("asdfasdfasdfd");
        });
  }

  private static AbstractObjectAssert<?, Object> getSameAs(AssertableApplicationContext context) {
    AbstractObjectAssert<?, Object> eventService1 = assertThat(context).getBean("eventService");
    EventService bean = context.getBean(EventService.class);
    AbstractObjectAssert<?, Object> eventService = eventService1.isSameAs(bean);
    return eventService;
  }

  private static ApplicationContextAssert<ConfigurableApplicationContext> getAnAssert(AssertableApplicationContext context) {
    ApplicationContextAssert<ConfigurableApplicationContext> hasEventService = assertThat(context).hasSingleBean(EventService.class);
    return hasEventService;
  }
}
