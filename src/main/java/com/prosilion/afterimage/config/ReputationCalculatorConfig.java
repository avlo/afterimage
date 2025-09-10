package com.prosilion.afterimage.config;

import com.prosilion.afterimage.service.AfterimageReputationCalculator;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.user.Identity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class ReputationCalculatorConfig {

  @Bean
  public AfterimageReputationCalculator afterimageReputationCalculator(
      @NonNull Identity aImgIdentity,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    return new AfterimageReputationCalculator(aImgIdentity, reputationBadgeDefinitionEvent);
  }
}

