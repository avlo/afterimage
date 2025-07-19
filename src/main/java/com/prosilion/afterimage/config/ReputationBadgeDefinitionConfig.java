package com.prosilion.afterimage.config;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.ReferenceTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.type.EventPluginIF;
import com.prosilion.superconductor.lib.jpa.dto.GenericEventKindDto;
import java.net.URISyntaxException;
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
      @NonNull String afterimageRelayUrl) throws NoSuchAlgorithmException, URISyntaxException {

    BadgeDefinitionEvent reputationBadgeDefinitionEvent = new BadgeDefinitionEvent(
        afterimageInstanceIdentity,
        new IdentifierTag(AfterimageKindType.REPUTATION.getName()),
        new ReferenceTag(afterimageRelayUrl),
        "afterimage reputation definition f(x)");

    eventPlugin.processIncomingEvent(
        new GenericEventKindDto(reputationBadgeDefinitionEvent).convertBaseEventToGenericEventKindIF());

    return reputationBadgeDefinitionEvent;
  }
}
