//package com.prosilion.afterimage.util;
//
//import com.prosilion.afterimage.event.VoteTag;
//import java.math.BigDecimal;
//import java.util.List;
//import lombok.NonNull;
//
//public class ReputationCalculator {
//
//  public static Integer calculateReputation(@NonNull List<VoteTag> voteTags) {
//    return voteTags.stream()
//        .map(VoteTag::getIdentifierTag)
//        .reduce((identifierTag, identifierTag2) -> 
//            identifierTag.).orElse(0);
//  }
//}
