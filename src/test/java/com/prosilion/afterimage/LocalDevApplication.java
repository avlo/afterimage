package com.prosilion.afterimage;

import com.prosilion.afterimage.config.LocalDevTestcontainersConfig;
import org.springframework.boot.SpringApplication;

public class LocalDevApplication {
  public static void main(String[] args) {
    SpringApplication.from(AfterimageApplication::main)
        .with(LocalDevTestcontainersConfig.class)
        .run(args);
  }
}
