package com.prosilion.afterimage.config;

import com.prosilion.afterimage.calculator.ReputationCalculatorIF;
import com.prosilion.afterimage.calculator.UnitReputationCalculator;
import com.prosilion.afterimage.service.reputation.CalculatorLocalService;
import com.prosilion.afterimage.service.reputation.CalculatorServiceIF;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.user.Identity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class CalculatorServicesConfig {
  @Bean
  CalculatorServiceIF calculatorLocalService() {
    return new CalculatorLocalService();
  }

  @Bean
  ReputationCalculatorIF unitReputationCalculator(
      @NonNull CalculatorServiceIF calculatorLocalService,
      @NonNull Identity aImgIdentity,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    UnitReputationCalculator unitReputationCalculator = new UnitReputationCalculator(calculatorLocalService, aImgIdentity, reputationBadgeDefinitionEvent);
    return unitReputationCalculator;
  }
  
/*
CONTROLLER / REST Web-API related beans, back-burner
 */
//  @Bean
//  public CalculatorController<String> reputationMicroServiceController(@NonNull CalculatorLocalService calculatorLocalService) {
//    return new CalculatorController<>(calculatorLocalService);
//  }

//  @Bean
//  URL calculatorMicroserviceUrl(@NonNull @Value("${afterimage.service.calculator.url}") String calculatorUrl) throws MalformedURLException {
//    return URI.create(calculatorUrl).toURL();
//  }

//  @Bean
//  CalculatorMicroService calculationMicroService(URL calculatorMicroserviceUrl) throws URISyntaxException {
//    return new CalculatorMicroService(calculatorMicroserviceUrl);
//  }

}

