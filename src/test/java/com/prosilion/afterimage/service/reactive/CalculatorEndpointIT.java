//package com.prosilion.afterimage.service.reactive;
//
//import com.prosilion.afterimage.config.ScoreVoteEvents;
//import com.prosilion.afterimage.service.reputation.CalculatorMicroService;
//import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
//import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
//import java.math.BigDecimal;
//import java.util.List;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.lang.NonNull;
//import org.springframework.test.context.ActiveProfiles;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@Slf4j
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
//@ActiveProfiles("test")
//@EmbeddedRedisStandalone
//public class CalculatorEndpointIT {
//  private final CalculatorMicroService calculatorMicroService;
//
//  @Autowired
//  public CalculatorEndpointIT(@NonNull CalculatorMicroService calculatorMicroService) {
//    this.calculatorMicroService = calculatorMicroService;
//  }
//
//  @Test
//  void testCalculatorEndpointAvailability() {
//    BigDecimal calculate = calculatorMicroService.calculate(
//        new ScoreVoteEvents(BigDecimal.ZERO,
//            List.of(
//                SuperconductorKindType.UNIT_UPVOTE.getName(),
//                SuperconductorKindType.UNIT_UPVOTE.getName())));
//    assertEquals(BigDecimal.TWO, calculate);
//  }
//}
