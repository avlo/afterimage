package com.prosilion.afterimage.config.web;

import com.prosilion.superconductor.autoconfigure.base.web.event.EventApiAuthUiIF;

public class EventApiAuthUi implements EventApiAuthUiIF {
  @Override
  public String getEventAuthHtmlFile() {
    return "thymeleaf/api-tests-auth";
  }
}
