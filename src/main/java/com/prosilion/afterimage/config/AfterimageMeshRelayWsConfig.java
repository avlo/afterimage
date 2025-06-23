package com.prosilion.afterimage.config;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.relay.AfterimageMeshRelayService;
import com.prosilion.nostr.codec.deserializer.EventMessageDeserializer;
import com.prosilion.nostr.enums.KindTypeIF;
import java.util.List;
import lombok.NonNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class AfterimageMeshRelayWsConfig extends AfterimageMeshRelayBaseConfig {
  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public AfterimageMeshRelayService afterimageReactiveRelayClient(@NonNull String afterimageRelayUrl) {
    return new AfterimageMeshRelayService(afterimageRelayUrl);
  }

  @Bean
  List<KindTypeIF> kindTypes() {
    return List.of(AfterimageKindType.values());
  }

  @Bean
  public EventMessageDeserializer eventMessageDeserializer() {
    return new EventMessageDeserializer(kindTypes());
  }
}
