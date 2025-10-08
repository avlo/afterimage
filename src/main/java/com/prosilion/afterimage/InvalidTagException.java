package com.prosilion.afterimage;

import com.prosilion.nostr.NostrException;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class InvalidTagException extends NostrException {
  public static final String message = "Filter AddressTag KindTYpeIF [%s] not supported.  must be one of KindTypeIF [%s]";

  public InvalidTagException(String invalidKindType, String... validTags) {
    this(invalidKindType, List.of(validTags));
  }

  public InvalidTagException(String invalidKindType, List<String> validTags) {
    super(
        String.format(
            message,
            invalidKindType,
            Strings.join(validTags, ',')));
  }
}
