package com.prosilion.afterimage;

import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class InvalidKindException extends RuntimeException {
  public static final String message = "KindType Request Kind [%s] not supported.  must be one of Kind [%s]";

  public InvalidKindException(String invalidKind, List<String> validKinds) {
    super(
        String.format(
            message,
            invalidKind,
            Strings.join(validKinds, ',')));
  }
}
