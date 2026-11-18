package com.prosilion.afterimage.service.proxy;

import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.subdivisions.client.RequestSubscriberDelegateIF;
import java.util.Set;
import lombok.NonNull;

public interface RelayMeshProxyIF extends RequestSubscriberDelegateIF<BaseMessage> {
  void activateRequestFlux(@NonNull Filters filters, @NonNull Set<Relay> relays);
  void activateRequestFlux(@NonNull Filters filters, @NonNull Relay relay);
}
