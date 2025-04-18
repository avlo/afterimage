package com.prosilion.afterimage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.prosilion.afterimage.*", "com.prosilion.superconductor.*"})
@EntityScan(basePackages = {"com.prosilion.superconductor.*"})
@EnableJpaRepositories("com.prosilion.superconductor.repository")
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
