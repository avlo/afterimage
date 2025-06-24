package com.prosilion.afterimage.config;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.request.AfterimageMessageReqService;
import com.prosilion.afterimage.request.ReqKindTypePlugin;
import com.prosilion.nostr.codec.deserializer.EventMessageDeserializer;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.service.message.req.ReqMessageServiceIF;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

@Slf4j
public class AfterimageMeshRelayBaseConfig {
  @Bean
  Identity afterimageInstanceIdentity(@NonNull @Value("${afterimage.key.private}") String privateKey) {
    return Identity.create(privateKey);
  }

  @Bean
  @Primary
  ReqMessageServiceIF reqMessageServiceIF(
      @NonNull List<ReqKindTypePlugin> eventTypePlugins,
      @NonNull ReqMessageServiceIF reqMessageService) {
    return new AfterimageMessageReqService(eventTypePlugins, reqMessageService);
  }

  @Bean
  List<KindTypeIF> kindTypes() {
    log.info("Loading custom AfterImage kind types [{}]", (Object[]) AfterimageKindType.values());
    return List.of(AfterimageKindType.values());
  }

  @Bean
  public EventMessageDeserializer eventMessageDeserializer(List<KindTypeIF> kindTypes) {
    EventMessageDeserializer eventMessageDeserializer = new EventMessageDeserializer(kindTypes);
    log.info("EventMessageDeserializer instance [{}]", eventMessageDeserializer);
    return eventMessageDeserializer;
  }
}
