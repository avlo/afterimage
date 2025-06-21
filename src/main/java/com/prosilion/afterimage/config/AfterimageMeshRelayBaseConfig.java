package com.prosilion.afterimage.config;

import com.prosilion.afterimage.request.AfterimageRequestService;
import com.prosilion.afterimage.request.ReqKindTypePlugin;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.service.request.ReqServiceIF;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

public class AfterimageMeshRelayBaseConfig {
  @Bean
  Identity afterimageInstanceIdentity(@NonNull @Value("${afterimage.key.private}") String privateKey) {
    return Identity.create(privateKey);
  }

  @Bean
  @Primary
  ReqServiceIF reqServiceIF(
      @NonNull List<ReqKindTypePlugin> eventTypePlugins,
      @NonNull ReqServiceIF reqService) {
    return new AfterimageRequestService(eventTypePlugins, reqService);
  }
}
