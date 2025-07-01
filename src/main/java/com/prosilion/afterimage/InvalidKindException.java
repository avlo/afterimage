package com.prosilion.afterimage;

import com.prosilion.nostr.NostrException;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class InvalidKindException extends NostrException {
  public static final String message = "KindType Request Kind [%s] not supported.  must be one of Kind [%s]";

  public InvalidKindException(String invalidKind, List<String> validKinds) {
    super(
        String.format(
            message,
            invalidKind,
            Strings.join(validKinds, ',')));
  }
}
