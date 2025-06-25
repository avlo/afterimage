package com.prosilion.afterimage.request;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import java.util.List;
import lombok.NonNull;

public interface ReqKindPlugin {
  Filters processIncomingRequest(@NonNull List<Filters> filtersList);

  Kind getKind();
}
