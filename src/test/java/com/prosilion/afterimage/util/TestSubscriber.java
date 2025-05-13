package com.prosilion.afterimage.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.NonNull;
import org.awaitility.Awaitility;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

public class TestSubscriber<T> extends BaseSubscriber<T> {
  public enum Mode {
    WAIT_FOR_COMPLETE_ATOMIC_BOOL__CANONICAL_MODE,
    DO_NOT_WAIT_FOR_COMPLETE_ATOMIC_BOOL__FLUX_KNOWN_TO_HAVE_NO_RETURNED_ITEMS__NEEDS_FIXING
  }

  private final List<T> items = Collections.synchronizedList(new ArrayList<>());
  private final AtomicBoolean completed;
  private Subscription subscription;

  public TestSubscriber() {
    this(Mode.WAIT_FOR_COMPLETE_ATOMIC_BOOL__CANONICAL_MODE);
  }

  public TestSubscriber(Mode mode) {
    super();
    this.completed = mode.equals(
        Mode.DO_NOT_WAIT_FOR_COMPLETE_ATOMIC_BOOL__FLUX_KNOWN_TO_HAVE_NO_RETURNED_ITEMS__NEEDS_FIXING) ? 
        new AtomicBoolean(true) : 
        new AtomicBoolean(false);
  }

  @Override
  public void hookOnSubscribe(@NonNull Subscription subscription) {
    this.subscription = subscription;
    subscription.request(1);
  }

  @Override
  public void hookOnNext(@NonNull T value) {
    completed.set(false);
    subscription.request(1);
    completed.set(true);
    items.add(value);
  }

  public List<T> getItems() {
    Awaitility.await()
        .timeout(5, TimeUnit.SECONDS)
        .untilTrue(completed);
    List<T> eventList = List.copyOf(items);
    items.clear();
    return eventList;
  }

  @Override
  protected void hookOnComplete() {
    completed.set(true);
  }
}
