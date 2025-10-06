package com.prosilion.afterimage.config.web;

import com.prosilion.superconductor.autoconfigure.base.web.req.ReqApiAuthUiIF;

public class ReqApiAuthUi implements ReqApiAuthUiIF {
  @Override
  public String getReqAuthHtmlFile() {
    return "thymeleaf/request-test-auth";
  }
}
