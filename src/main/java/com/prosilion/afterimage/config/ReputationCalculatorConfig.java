package com.prosilion.afterimage.config;

import com.prosilion.afterimage.InvalidReputationCalculatorException;
import com.prosilion.afterimage.calculator.ReputationCalculatorIF;
import com.prosilion.afterimage.service.reputation.ReputationCalculationLocalService;
import com.prosilion.afterimage.service.reputation.ReputationCalculationServiceIF;
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
public class ReputationCalculatorConfig {
  @Bean
  ReputationCalculatorIF reputationCalculatorIF(
      @NonNull @Value("${afterimage.calculator.impl}") String calculator,
      @NonNull List<ReputationCalculatorIF> calculatorIFS) {
    return Optional.ofNullable(
            calculatorIFS.stream().collect(
                    Collectors.toMap(
                        ReputationCalculatorIF::getFullyQualifiedCalculatorName,
                        Function.identity(),
                        (prev, next) -> next, HashMap::new))
                .get(calculator))
        .orElseThrow(() ->
            new InvalidReputationCalculatorException(calculator, calculatorIFS.stream().map(ReputationCalculatorIF::getFullyQualifiedCalculatorName).collect(Collectors.toList())));
  }

  @Bean
  public ReputationCalculationServiceIF reputationCalculationServiceIF(
      @NonNull ReputationCalculatorIF reputationCalculatorIF) {
    return new ReputationCalculationLocalService(reputationCalculatorIF);
  }
}

