package com.prosilion.afterimage.config;

import com.prosilion.afterimage.service.AfterimageMeshRelayService;
import com.prosilion.afterimage.service.ReputationReqMessageService;
import com.prosilion.afterimage.util.Factory;
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
  Identity afterimageInstanceIdentity() {
    return Factory.createNewIdentity();
  }

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public AfterimageMeshRelayService afterimageReactiveRelayClient(@NonNull @Value("${afterimage.relay.url}") String relayUri) {
    return new AfterimageMeshRelayService(relayUri);
  }

  @Bean
  @Primary
  public ReqMessageServiceIF<ReqMessage> reputationReqMessageService(ReqMessageServiceIF<ReqMessage> reqMessageService) {
    return new ReputationReqMessageService<>(reqMessageService);
  }
}
