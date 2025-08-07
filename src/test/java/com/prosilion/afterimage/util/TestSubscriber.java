package com.prosilion.afterimage.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.SignalType;
import reactor.util.context.Context;

@Slf4j
public class TestSubscriber<T> extends BaseSubscriber<T> {
  private final List<T> items = Collections.synchronizedList(new ArrayList<>());
  private final AtomicBoolean completed = new AtomicBoolean(false);
  private Subscription subscription;

  @Override
  public void hookOnSubscribe(@NonNull Subscription subscription) {
//    log.debug("in TestSubscriber.hookOnSubscribe()");
    this.subscription = subscription;
    subscription.request(1);
  }

  @Override
  public void hookOnNext(@NonNull T value) {
//    log.debug("in TestSubscriber.hookOnNext()");
    subscription.request(1);
    completed.set(true);
    items.add(value);
  }

  public List<T> getItems() {
//    log.debug("in TestSubscriber.getItems()");
    Awaitility.await()
        .timeout(5, TimeUnit.SECONDS)
        .untilTrue(completed);
//    List<T> eventList = List.copyOf(items);
//    items.clear();
    return items;
  }

  //    below included only informatively / as reminder of their existence
  @Override
  protected void hookOnCancel() {
//    log.debug("in TestSubscriber.hookOnCancel()");
    super.hookOnCancel();
  }

  @Override
  protected void hookOnComplete() {
//    log.debug("in TestSubscriber.hookOnComplete()");
    completed.setRelease(true);
    super.hookOnComplete();
  }

  @Override
  protected void hookOnError(@NonNull Throwable throwable) {
//    log.debug("in TestSubscriber.hookOnError()");
    super.hookOnError(throwable);
  }

  @Override
  protected void hookFinally(@NonNull SignalType type) {
//    log.debug("in TestSubscriber.hookFinally()");
    super.hookFinally(type);
  }

  @Override
  public void dispose() {
//    log.debug("in TestSubscriber.dispose()");
    super.dispose();
  }

  @Override
  public boolean isDisposed() {
//    log.debug("in TestSubscriber.isDisposed()");    
    return super.isDisposed();
  }

  @Override
  protected @NonNull Subscription upstream() {
//    log.debug("in TestSubscriber.upstream()");    
    return super.upstream();
  }

  @Override
  public @NonNull Context currentContext() {
//    log.debug("in TestSubscriber.currentContext()");    
    return super.currentContext();
  }
}
