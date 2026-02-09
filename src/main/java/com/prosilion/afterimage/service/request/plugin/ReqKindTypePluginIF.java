package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import java.util.List;
import lombok.NonNull;

public interface ReqKindTypePluginIF {
  Filters processIncomingRequest(@NonNull List<Filters> filtersList) throws NostrException;
  Kind getKind();
  KindTypeIF getKindType();
}
