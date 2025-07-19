package com.prosilion.afterimage.service;

import com.prosilion.superconductor.base.service.event.EventServiceIF;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EventServiceIT {
  private static final Logger log = LoggerFactory.getLogger(EventServiceIT.class);
  private final EventServiceIF eventService;

  @Autowired
  public EventServiceIT(@NonNull EventServiceIF eventService) {
    this.eventService = eventService;
    log.info("EventServiceIT initialized, services: {}", this.eventService);
  }

  @Test
  void testEventService() {
    log.debug(eventService.toString());
  }
}
