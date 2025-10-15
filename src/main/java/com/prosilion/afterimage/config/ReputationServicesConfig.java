package com.prosilion.afterimage.config;

import com.prosilion.afterimage.InvalidReputationCalculatorException;
import com.prosilion.afterimage.calculator.ReputationCalculatorIF;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class ReputationServicesConfig {
  @Bean
  ReputationCalculatorIF reputationCalculatorIF(
      @NonNull @Value("${afterimage.calculator.impl}") String calculator,
      @NonNull List<ReputationCalculatorIF> reputationCalculatorIFS) {
    ReputationCalculatorIF reputationCalculatorIF = Optional.ofNullable(
            reputationCalculatorIFS.stream().collect(
                    Collectors.toMap(
                        ReputationCalculatorIF::getFullyQualifiedCalculatorName,
                        Function.identity(),
                        (prev, next) -> next, HashMap::new))
                .get(calculator))
        .orElseThrow(() ->
            new InvalidReputationCalculatorException(calculator, reputationCalculatorIFS.stream().map(ReputationCalculatorIF::getFullyQualifiedCalculatorName).collect(Collectors.toList())));
    return reputationCalculatorIF;
  }
}

