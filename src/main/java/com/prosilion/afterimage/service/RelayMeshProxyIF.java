package com.prosilion.afterimage.service;

import com.prosilion.nostr.filter.Filters;
import java.util.List;
import org.springframework.lang.NonNull;

public interface RelayMeshProxyIF {
  void activateRequestFlux(@NonNull Filters filters, @NonNull List<String> relayUrl);
  void activateRequestFlux(@NonNull Filters filters, @NonNull String relayUrl);
}
