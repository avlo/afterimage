package com.prosilion.afterimage.config;

import com.prosilion.afterimage.relay.AfterimageMeshRelayService;
import com.prosilion.afterimage.relay.ReputationReqMessageService;
import com.prosilion.superconductor.service.message.req.ReqMessageServiceIF;
import lombok.NonNull;
import nostr.event.message.ReqMessage;
import nostr.id.Identity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

@Lazy
@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class AfterimageMeshRelayWsConfig {

  @Bean
  Identity afterimageInstanceIdentity(@NonNull @Value("${afterimage.key.private}") String privateKey) {
    return Identity.create(privateKey);
  }

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public AfterimageMeshRelayService afterimageReactiveRelayClient(@NonNull String afterimageRelayUrl) {
    return new AfterimageMeshRelayService(afterimageRelayUrl);
  }

  @Bean
  @Primary
  public ReqMessageServiceIF<ReqMessage> reputationReqMessageService(
      @NonNull ReqMessageServiceIF<ReqMessage> reqMessageService,
      @NonNull Identity aImgIdentity) {
    return new ReputationReqMessageService<>(reqMessageService, aImgIdentity);
  }
}
