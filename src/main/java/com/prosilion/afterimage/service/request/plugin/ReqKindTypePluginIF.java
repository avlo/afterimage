package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.filter.Filters;
import java.util.List;
import lombok.NonNull;

public interface ReqKindTypePluginIF<KindTypeIF> {
  Filters processIncomingRequest(@NonNull List<Filters> filtersList);
  Kind getKind();
  KindTypeIF getKindType();
}
