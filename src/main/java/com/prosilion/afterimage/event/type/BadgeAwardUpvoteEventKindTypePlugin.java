//package com.prosilion.afterimage.event.type;
//
//import com.prosilion.afterimage.enums.AfterimageKindType;
//import com.prosilion.nostr.enums.Kind;
//import com.prosilion.nostr.enums.KindTypeIF;
//import com.prosilion.nostr.event.GenericEventKindTypeIF;
//import com.prosilion.superconductor.service.event.type.AbstractEventKindPlugin;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.springframework.stereotype.Component;
//
//@Component
//public class BadgeAwardUpvoteEventKindTypePlugin implements AbstractEventKindPlugin {
//  private static final Log log = LogFactory.getLog(BadgeAwardUpvoteEventKindTypePlugin.class);
//
//  @Override
//  public void processIncomingEvent(GenericEventKindTypeIF event) {
//    log.debug(String.format("processing incoming UPVOTE EVENT: [%s]", event.getKind()));
//  }
//
//  @Override
//  public Kind getKind() {
//    return Kind.BADGE_AWARD_EVENT;
//  }
//
//  @Override
//  public KindTypeIF getKindType() {
//    return AfterimageKindType.UPVOTE;
//  }
//}
