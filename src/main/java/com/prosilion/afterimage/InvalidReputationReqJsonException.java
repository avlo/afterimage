package com.prosilion.afterimage;

import com.prosilion.nostr.filter.Filters;
import com.prosilion.superconductor.util.EmptyFiltersException;
import java.util.List;

public class InvalidReputationReqJsonException extends EmptyFiltersException {
  public final static String INVALID_FILTERS = "Invalid ReqMessage JSON filters: [%s] does not contain required [%s] tag";

  public InvalidReputationReqJsonException(List<Filters> filtersList, String type) {
    super(String.format(INVALID_FILTERS, filtersList.toString(), type));
  }
}
