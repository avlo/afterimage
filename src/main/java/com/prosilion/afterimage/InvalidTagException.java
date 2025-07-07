package com.prosilion.afterimage;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.tag.BaseTag;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class InvalidTagException extends RuntimeException {
  public static final String message = "Filter AddressTag KindTYpeIF [%s] not supported.  must be one of KindTypeIF [%s]";

  public InvalidTagException(String invalidKindType, List<String> validTags) {
    super(
        String.format(
            message,
            invalidKindType,
            Strings.join(validTags, ',')));
  }
}
