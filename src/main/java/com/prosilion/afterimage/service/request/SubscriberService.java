package com.prosilion.afterimage.service.request;

import com.prosilion.afterimage.entity.Subscriber;
import com.prosilion.afterimage.util.EmptyFiltersException;
import com.prosilion.afterimage.util.NoExistingUserException;
import lombok.NonNull;
import nostr.event.filter.Filters;

import java.util.List;
import java.util.Map;

public interface SubscriberService {
  Long save(@NonNull Subscriber subscriber, @NonNull List<Filters> filtersList) throws EmptyFiltersException;

  List<Long> removeSubscriberBySessionId(@NonNull String sessionId);

  Long removeSubscriberBySubscriberId(@NonNull String subscriberId) throws NoExistingUserException;

  List<Filters> getFiltersList(@NonNull Long subscriberId);

  Map<Long, List<Filters>> getAllFiltersOfAllSubscribers();

  Subscriber get(@NonNull Long subscriberHash);
}
