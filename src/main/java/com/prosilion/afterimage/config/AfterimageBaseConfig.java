package com.prosilion.afterimage.config;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.relay.AfterimageReqService;
import com.prosilion.afterimage.service.event.plugin.DownvoteEventKindTypePlugin;
import com.prosilion.afterimage.service.event.plugin.ReputationPublishingEventKindTypePlugin;
import com.prosilion.afterimage.service.event.plugin.SuperconductorFollowsListNonPublishingEventKindPlugin;
import com.prosilion.afterimage.service.event.plugin.UpvoteEventKindTypePlugin;
import com.prosilion.afterimage.service.request.ReqKindServiceIF;
import com.prosilion.afterimage.service.request.ReqKindTypeServiceIF;
import com.prosilion.nostr.codec.deserializer.EventMessageDeserializer;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.service.event.service.EventKindTypeServiceIF;
import com.prosilion.superconductor.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.service.event.service.plugin.EventKindTypePlugin;
import com.prosilion.superconductor.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.EventKindPlugin;
import com.prosilion.superconductor.service.event.type.EventPluginIF;
import com.prosilion.superconductor.service.event.type.SuperconductorKindType;
import com.prosilion.superconductor.service.request.NotifierService;
import com.prosilion.superconductor.service.request.ReqServiceIF;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

@Slf4j
public abstract class AfterimageBaseConfig {
  @Bean
  Identity afterimageInstanceIdentity(@NonNull @Value("${afterimage.key.private}") String privateKey) {
    return Identity.create(privateKey);
  }

  @Bean
  @Primary
  ReqServiceIF afterimageReqService(
      @NonNull ReqServiceIF reqService,
      @NonNull ReqKindServiceIF reqKindService,
      @NonNull ReqKindTypeServiceIF reqKindTypeService) {
    return new AfterimageReqService(reqService, reqKindService, reqKindTypeService);
  }

  @Bean
  List<KindTypeIF> kindTypes() {
    List<KindTypeIF> values = List.of(AfterimageKindType.values());
    log.info("Loading custom AfterImage kind types [{}]", values);
    return values;
  }

  @Bean
  public EventMessageDeserializer eventMessageDeserializer() {
    EventMessageDeserializer eventMessageDeserializer = new EventMessageDeserializer();
    log.info("EventMessageDeserializer instance [{}]", eventMessageDeserializer);
    return eventMessageDeserializer;
  }

  @Bean
  EventKindTypePluginIF<KindTypeIF> reputationEventKindTypePlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventPluginIF eventPlugin,
      @NonNull EventEntityService eventEntityService,
      @NonNull Identity aImgIdentity,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    return new ReputationPublishingEventKindTypePlugin(
        notifierService,
        new EventKindTypePlugin(
            AfterimageKindType.REPUTATION,
            eventPlugin),
        eventEntityService,
        aImgIdentity,
        reputationBadgeDefinitionEvent);
  }

  @Bean
  EventKindTypePluginIF<KindTypeIF> upvoteEventKindTypePlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull EventKindTypePluginIF<KindTypeIF> reputationEventKindTypePlugin) {
    return new UpvoteEventKindTypePlugin(
        new EventKindTypePlugin(
            SuperconductorKindType.UPVOTE,
            eventPlugin),
        reputationEventKindTypePlugin);
  }

  @Bean
  EventKindTypePluginIF<KindTypeIF> downvoteEventKindTypePlugin(
      @NonNull EventPluginIF eventPlugin,
      @NonNull EventKindTypePluginIF<KindTypeIF> reputationEventKindTypePlugin) {
    return new DownvoteEventKindTypePlugin(
        new EventKindTypePlugin(
            SuperconductorKindType.DOWNVOTE,
            eventPlugin),
        reputationEventKindTypePlugin);
  }

  @Bean
  EventKindPluginIF<Kind> superconductorFollowsListNonPublishingEventKindPlugin(
      @NonNull EventEntityService eventEntityService,
      @NonNull EventKindTypeServiceIF eventKindTypeService,
      @NonNull Identity aImgIdentity,
      @NonNull EventPluginIF eventPlugin) {
    return new SuperconductorFollowsListNonPublishingEventKindPlugin(
        new EventKindPlugin(
            Kind.RELAY_LIST_METADATA,
            eventPlugin),
        eventKindTypeService,
        eventEntityService,
        aImgIdentity);
  }
}
