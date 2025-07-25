package com.prosilion.afterimage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.prosilion.afterimage.*", "com.prosilion.superconductor.*"})
@EntityScan(basePackages = {"com.prosilion.superconductor.*"})
public class AfterimageApplication extends SpringBootServletInitializer {

  /**
   * spring-boot WAR hook
   */
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(AfterimageApplication.class);
  }

  /**
   * spring-boot executable JAR hook
   */
  public static void main(String[] args) {
    SpringApplication
//        .from(AfterimageApplication::main)
//        .with(SuperconductorRelaysConfig.class)
//        .run(args);
        .run(AfterimageApplication.class, args);
  }
}
