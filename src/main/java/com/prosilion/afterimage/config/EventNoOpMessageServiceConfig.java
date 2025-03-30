package com.prosilion.afterimage.config;

import com.prosilion.afterimage.service.clientresponse.ClientResponseService;
import com.prosilion.afterimage.service.message.MessageService;
import com.prosilion.afterimage.service.noop.EventNoOpMessageService;
import lombok.extern.slf4j.Slf4j;
import nostr.event.message.EventMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "afterimage.noop.event", havingValue = "true")
public class EventNoOpMessageServiceConfig {
  @Bean
  MessageService<EventMessage> getEventMessageService(
      ClientResponseService clientResponseService,
      @Value("${afterimage.noop.event.description}") String noOp) {
    log.debug("loaded EventNoOpMessageService bean (NO_OP_EVENT)");
    return new EventNoOpMessageService<>(clientResponseService, noOp);
  }
}
