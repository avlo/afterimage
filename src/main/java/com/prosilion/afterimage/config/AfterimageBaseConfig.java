package com.prosilion.afterimage.config;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.relay.AfterimageReqService;
import com.prosilion.afterimage.service.event.plugin.ReputationEventKindTypePlugin;
import com.prosilion.afterimage.service.request.ReqKindServiceIF;
import com.prosilion.afterimage.service.request.ReqKindTypeServiceIF;
import com.prosilion.nostr.codec.deserializer.EventMessageDeserializer;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.service.event.service.plugin.EventKindTypePlugin;
import com.prosilion.superconductor.service.event.type.EventPluginIF;
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
    log.info("Loading custom AfterImage kind types [{}]", (Object[]) AfterimageKindType.values());
    return List.of(AfterimageKindType.values());
  }

  @Bean
  public EventMessageDeserializer eventMessageDeserializer() {
    EventMessageDeserializer eventMessageDeserializer = new EventMessageDeserializer();
    log.info("EventMessageDeserializer instance [{}]", eventMessageDeserializer);
    return eventMessageDeserializer;
  }

  @Bean
  ReputationEventKindTypePlugin reputationEventKindTypePlugin(
      @NonNull NotifierService notifierService,
      @NonNull EventPluginIF eventPlugin) {
    return new ReputationEventKindTypePlugin(
        notifierService,
        new EventKindTypePlugin(
            AfterimageKindType.REPUTATION,
            eventPlugin));
  }
}
