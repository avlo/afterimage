package com.prosilion.afterimage.service;

import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.subdivisions.client.RequestSubscriberDelegateIF;
import java.util.Set;
import org.springframework.lang.NonNull;

public interface RelayMeshProxyIF extends RequestSubscriberDelegateIF<BaseMessage> {
  void activateRequestFlux(@NonNull Filters filters, @NonNull Set<String> relayUrl);
  void activateRequestFlux(@NonNull Filters filters, @NonNull String relayUrl);
}
