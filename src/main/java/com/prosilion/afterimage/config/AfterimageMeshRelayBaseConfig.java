package com.prosilion.afterimage.config;

import com.prosilion.afterimage.request.AfterimageRequestService;
import com.prosilion.afterimage.request.ReqKindTypePlugin;
import com.prosilion.superconductor.service.request.ReqServiceIF;
import java.util.List;
import lombok.NonNull;
import nostr.event.Kind;
import nostr.event.impl.GenericEvent;
import nostr.id.Identity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

public class AfterimageMeshRelayBaseConfig {
  @Bean
  Identity afterimageInstanceIdentity(@NonNull @Value("${afterimage.key.private}") String privateKey) {
    return Identity.create(privateKey);
  }

  @Bean
  @Primary
  ReqServiceIF<GenericEvent> reqServiceIF(
      @NonNull List<ReqKindTypePlugin<Kind>> eventTypePlugins,
      @NonNull ReqServiceIF<GenericEvent> reqService) {
    return new AfterimageRequestService<>(eventTypePlugins, reqService);
  }
}
