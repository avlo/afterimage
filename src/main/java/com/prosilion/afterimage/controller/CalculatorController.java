//package com.prosilion.afterimage.controller;
//
//import com.prosilion.afterimage.config.ScoreVoteEvents;
//import com.prosilion.afterimage.service.reputation.CalculatorLocalService;
//import com.prosilion.afterimage.service.reputation.CalculatorServiceIF;
//import java.math.BigDecimal;
//import java.util.List;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.lang.NonNull;
//import org.springframework.web.bind.annotation.RequestBody;
//import reactor.core.publisher.BaseSubscriber;
//
//@Slf4j
////@RestController
//public class CalculatorController<T> extends BaseSubscriber<T> implements CalculatorServiceIF {
//  private final CalculatorLocalService calculatorLocalService;
//
//  public CalculatorController(@NonNull CalculatorLocalService calculatorLocalService) {
//    this.calculatorLocalService = calculatorLocalService;
//  }
//
//  //  @PostMapping("api/calculate")
////  @ResponseBody
//  public BigDecimal calculate(@RequestBody ScoreVoteEvents scoreVoteEvents) {
//    BigDecimal previousScore = scoreVoteEvents.previousScore();
//    List<String> voteEvents = scoreVoteEvents.voteEvents();
//    return calculatorLocalService.calculate(scoreVoteEvents);
//  }
//}
