package com.prosilion.afterimage.util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Lazy
@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "false")
public class NostrWsConfig {

//  @Bean
//  List<KindTypeIF> kindTypes() {
//    return List.of(AfterimageKindType.values());
//  }
//
//  @Bean
//  public EventMessageDeserializer eventMessageDeserializer() {
//    return new EventMessageDeserializer(kindTypes());
//  }

//  @Bean
//  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
//  public VoteEventKindTypePlugin voteEventKindTypePlugin() {
//    
//  }

//  public NostrRelayService nostrRelayService(@Value("${superconductor.relay.url}") String relayUri) throws ExecutionException, InterruptedException {
//    return new NostrRelayService(relayUri);
//  }
}
