package com.prosilion.afterimage.config;

import com.prosilion.afterimage.AfterimageApplication;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
//@Profile("!test")
public class LocalDevApplication {

  public static final String EXITS_APP = "x";
  public static final String EXIT_WHILE_LOOP = "m";

  public static void main(String[] args) throws IOException, InterruptedException {
    SpringApplication.Augmented ctxt = SpringApplication.from(AfterimageApplication::main).with(MultiContainerTestConfig.class);
    SpringApplication.Running run = ctxt.run(args);
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    clearWithLinuxCommand();

    
    boolean equals = false;
    while (!equals) {
      String pressed = pressAnyKeyToContinue(reader);
      if (pressed.equals(EXITS_APP)) {
        SpringApplication.exit(run.getApplicationContext(), () -> 0);
        log(EXIT_WHILE_LOOP);
      }
      equals = Objects.equals(EXIT_WHILE_LOOP, pressed);
    }
  }

  public static String pressAnyKeyToContinue(BufferedReader reader) throws IOException {
    // (Reuse the helper method from Section 3)
    System.out.print("\nPress any key to continue...");
    String name = reader.readLine();
    log(name);
    return name;
  }

  private static void log(String s) {
    int repeat = 10;
    System.out.println(StringUtils.repeat(s, 10));
    System.out.println(StringUtils.repeat(s, 10));
    System.out.println();
  }

  public static void clearWithLinuxCommand() throws IOException, InterruptedException {
    new ProcessBuilder("clear")
        .inheritIO()
        .start()
        .waitFor();
  }
}
