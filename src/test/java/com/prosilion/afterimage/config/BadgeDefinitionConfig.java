//package com.prosilion.afterimage.config;
//
//import com.prosilion.nostr.event.BadgeDefinitionAwardEvent;
//import com.prosilion.nostr.tag.IdentifierTag;
//import com.prosilion.nostr.user.Identity;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.lang.NonNull;
//
//import static com.prosilion.afterimage.util.TestKindType.UNIT_DOWNVOTE;
//import static com.prosilion.afterimage.util.TestKindType.UNIT_UPVOTE;
//
//@Configuration
//public class BadgeDefinitionConfig {
//
//  @Bean
//  BadgeDefinitionAwardEvent badgeDefinitionUpvoteEvent(@NonNull Identity superconductorInstanceIdentity) {
//    return new BadgeDefinitionAwardEvent(superconductorInstanceIdentity, new IdentifierTag(UNIT_UPVOTE.getName()));
//  }
//
//  @Bean
//  BadgeDefinitionAwardEvent badgeDefinitionDownvoteEvent(@NonNull Identity superconductorInstanceIdentity) {
//    return new BadgeDefinitionAwardEvent(superconductorInstanceIdentity, new IdentifierTag(UNIT_DOWNVOTE.getName()));
//  }
//}
