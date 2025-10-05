package com.prosilion.afterimage.config;

import java.util.Arrays;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class AfterimageAuthEventKindsCondition implements Condition {
  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    String kinds = context
        .getEnvironment()
        .getProperty("afterimage.auth.event.kinds", String.class, "");

    if (kinds.isEmpty()) {
      return false;
    }

    return !Arrays.stream(kinds.split(",")).toList().isEmpty();
  }
}
