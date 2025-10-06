package com.prosilion.afterimage.config.web;

import com.prosilion.superconductor.autoconfigure.base.web.event.EventApiNoAuthUiIF;

public class EventApiNoAuthUi implements EventApiNoAuthUiIF {
  @Override
  public String getEventNoAuthHtmlFile() {
    return "thymeleaf/api-tests";
  }
}
