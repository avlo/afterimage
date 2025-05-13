package com.prosilion.afterimage.exception;

import nostr.event.message.ReqMessage;

public class InvalidReputationReqJsonException extends RuntimeException {
  private final static String INVALID_FILTERS = "Invalid ReqMessage JSON filters:\n\n  %s\n\ndoes not contain required Author Tag and Vote tag";

  public InvalidReputationReqJsonException(ReqMessage message) {
    super(String.format(INVALID_FILTERS, message.getFiltersList().toString()));
  }
}
