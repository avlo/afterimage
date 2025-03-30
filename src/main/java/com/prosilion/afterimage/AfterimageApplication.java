package com.prosilion.afterimage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
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
    SpringApplication.run(AfterimageApplication.class, args);
  }
}
