package com.prosilion.afterimage.util;

import com.prosilion.nostr.filter.Filters;
import java.util.List;

public class InvalidReputationReqJsonException extends RuntimeException {
  public final static String INVALID_FILTERS = "Invalid ReqMessage JSON filters:\n  %s\ndoes not contain required\n  [%s] tag";

  public InvalidReputationReqJsonException(List<Filters> filtersList, String type) {
    super(String.format(INVALID_FILTERS, filtersList.toString(), type));
  }
}
