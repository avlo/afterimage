package com.prosilion.afterimage;

import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.filter.Filters;
import java.util.Arrays;
import java.util.List;

public class InvalidReputationReqJsonException extends NostrException {
  public final static String INVALID_FILTERS = "Invalid ReqMessage JSON filters: [%s] does not contain required [%s] tag";

  public InvalidReputationReqJsonException(List<Filters> filtersList, String type) {
    super(String.format(INVALID_FILTERS, Arrays.toString(filtersList.stream().map(Filters::toString).toArray(String[]::new)), type));
  }
}
