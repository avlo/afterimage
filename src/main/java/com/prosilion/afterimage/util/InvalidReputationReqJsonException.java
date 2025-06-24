package com.prosilion.afterimage.util;

import com.prosilion.nostr.message.ReqMessage;

public class InvalidReputationReqJsonException extends RuntimeException {
  public final static String INVALID_FILTERS = "Invalid ReqMessage JSON filters:\n  %s\ndoes not contain required\n  [%s] tag";

  public InvalidReputationReqJsonException(ReqMessage message, String type) {
    super(String.format(INVALID_FILTERS, message.getFiltersList().toString(), type));
  }
}
