package com.prosilion.afterimage.config;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.relay.AfterimageReqService;
import com.prosilion.afterimage.service.event.plugin.DownvoteEventKindTypePlugin;
import com.prosilion.afterimage.service.event.plugin.ReputationEventKindTypePlugin;
import com.prosilion.afterimage.service.event.plugin.SuperConductorRelayEnlistmentNonPublishingEventKindPlugin;
import com.prosilion.afterimage.service.event.plugin.UpvoteEventKindTypePlugin;
import com.prosilion.afterimage.service.request.ReqKindServiceIF;
import com.prosilion.afterimage.service.request.ReqKindTypeServiceIF;
import com.prosilion.nostr.codec.deserializer.EventMessageDeserializer;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.EventKindPlugin;
import com.prosilion.superconductor.service.event.type.EventPluginIF;
import com.prosilion.superconductor.service.event.type.NonPublishingEventKindPlugin;
import com.prosilion.superconductor.service.event.type.NonPublishingEventKindTypePlugin;
import com.prosilion.superconductor.service.event.type.PublishingEventKindTypePlugin;
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
      @NonNull EventEntityService eventEntityService,
      @NonNull Identity aImgIdentity,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
    return new PublishingEventKindTypePlugin(
        notifierService,
        new ReputationEventKindTypePlugin(
            eventEntityService,
            aImgIdentity,
            reputationBadgeDefinitionEvent));
  }

////  DECORATED
//  @Bean
//  EventKindTypePluginIF<KindTypeIF> reputationPublishingEventKindTypePlugin(
//      @NonNull NotifierService notifierService,
////  DECORATION      
//      @NonNull ReputationEventKindTypePlugin reputationEventKindTypePlugin) {

  /// /  DECORATOR
//    return new PublishingEventKindTypePlugin(notifierService, reputationEventKindTypePlugin);
//  }

//  DECORATED  
  @Bean
  EventKindTypePluginIF<KindTypeIF> upvoteEventKindTypePlugin(
      @NonNull EventEntityService eventEntityService,
      @NonNull EventKindTypePluginIF<KindTypeIF> reputationEventKindTypePlugin) {
    return new NonPublishingEventKindTypePlugin(
        new UpvoteEventKindTypePlugin(eventEntityService, reputationEventKindTypePlugin));
  }

  @Bean
  EventKindTypePluginIF<KindTypeIF> downvoteEventKindTypePlugin(
      @NonNull EventEntityService eventEntityService,
      @NonNull EventKindTypePluginIF<KindTypeIF> reputationEventKindTypePlugin) {
    return new NonPublishingEventKindTypePlugin(
        new DownvoteEventKindTypePlugin(eventEntityService, reputationEventKindTypePlugin));
  }

  @Bean
  NonPublishingEventKindPlugin superConductorRelayEnlistmentNonPublishingEventKindPlugin(
      @NonNull EventEntityService eventEntityService,
      @NonNull Identity aImgIdentity,
      @NonNull EventPluginIF eventPlugin) {
    return new SuperConductorRelayEnlistmentNonPublishingEventKindPlugin(
        new EventKindPlugin(
            Kind.GROUP_MEMBERS, eventPlugin), eventEntityService, aImgIdentity);
  }
}
