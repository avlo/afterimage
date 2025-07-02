package com.prosilion.afterimage.config;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.dto.GenericEventKindDto;
import com.prosilion.superconductor.service.event.type.EventPluginIF;
import java.security.NoSuchAlgorithmException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class ReputationBadgeDefinitionConfig {

  @Bean
  BadgeDefinitionEvent reputationBadgeDefinitionEvent(
      @NonNull EventPluginIF eventPlugin,
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull String afterimageRelayUrl) throws NoSuchAlgorithmException {

    BadgeDefinitionEvent reputationBadgeDefinitionEvent = new BadgeDefinitionEvent(
        afterimageInstanceIdentity,
        new IdentifierTag(AfterimageKindType.REPUTATION.getName()),
        new RelaysTag(new Relay(afterimageRelayUrl)),
        "afterimage reputation f(x)");

    eventPlugin.processIncomingEvent(
        new GenericEventKindDto(reputationBadgeDefinitionEvent).convertBaseEventToGenericEventKindIF());
    
    return reputationBadgeDefinitionEvent;
  }
}
