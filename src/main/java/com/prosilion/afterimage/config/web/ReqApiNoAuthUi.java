package com.prosilion.afterimage.config.web;

import com.prosilion.superconductor.autoconfigure.base.web.req.ReqApiNoAuthUiIF;

public class ReqApiNoAuthUi implements ReqApiNoAuthUiIF {
  @Override
  public String getReqNoAuthHtmlFile() {
    return "thymeleaf/request-test";
  }
}
