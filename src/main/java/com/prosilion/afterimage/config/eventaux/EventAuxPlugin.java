//package com.prosilion.afterimage.config.eventaux;
//
//import com.prosilion.nostr.NostrException;
//import com.prosilion.nostr.enums.Kind;
//import com.prosilion.nostr.event.BaseEvent;
//import com.prosilion.nostr.event.EventIF;
//import com.prosilion.nostr.event.GenericEventRecord;
//import com.prosilion.nostr.event.internal.Relay;
//import com.prosilion.nostr.tag.SetsPairedEventTagIF;
//import com.prosilion.superconductor.base.cache.CacheServiceIF;
//import com.prosilion.superconductor.base.service.event.plugin.EventPluginIF;
//import java.lang.reflect.Constructor;
//import java.lang.reflect.InvocationTargetException;
//import java.util.Map;
//import java.util.Optional;
//import java.util.function.BiFunction;
//import java.util.stream.Collectors;
//import lombok.Getter;
//import lombok.NonNull;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//public class EventAuxPlugin implements EventAuxPluginIF {
//  private final CacheServiceIF cacheServiceIF;
//  @Getter
//  private final Map<Kind, BiFunction<EventIF, Relay, Optional<? extends SetsPairedEventTagIF>>> eventAuxKindMaterializers;
//
//  public EventAuxPlugin(
//     @NonNull CacheServiceIF cacheServiceIF,
//     @NonNull Map<Kind, BiFunction<EventIF, Relay, Optional<? extends SetsPairedEventTagIF>>> eventAuxKindMaterializers) {
//    log.debug("class is adding cacheServiceIF implementation class: {}", cacheServiceIF.getClass().getSimpleName());
//    this.cacheServiceIF = cacheServiceIF;
//    this.eventAuxKindMaterializers = eventAuxKindMaterializers;
//    log.debug("loaded eventAuxKindMaterializers:\n{}", this.eventAuxKindMaterializers.entrySet().stream().map(entry ->
//       String.format("  %s : %s", entry.getKey().getName().toUpperCase(), entry.getValue())).collect(Collectors.joining(",\n")));
//  }
//
//  @Override
//  public Optional<GenericEventRecord> processIncomingEvent(@NonNull EventIF event, @NonNull Relay fromRelay) {
//    log.debug("processIncomingEvent() called with event\n{}", event.createPrettyPrintJson());
//    Optional<GenericEventRecord> eventAlreadyExists = eventAlreadyExistsFxn.apply(cacheServiceIF, event);
//    if (eventAlreadyExists.isPresent()) {
//      log.debug("event already exists in db, do not materialize, just return\n  {}\n", event.createPrettyPrintJson());
//      return eventAlreadyExists;
//    }
//    
//    
//
//    log.debug("kind/kindType event does not yet exist in db, materialize...\n  {}\n", event.createPrettyPrintJson());
//    Optional<? extends SetsPairedEventTagIF> apply = eventAuxKindMaterializers.get(event.getKind()).apply(event, fromRelay);
//    Optional<GenericEventRecord> genericEventRecord = apply.map(cacheServiceIF::save);
//    return genericEventRecord;
//  }
//
//  private <T extends BaseEvent> T createTypedAuxEvent(
//     @NonNull GenericEventRecord genericEventRecord,
//     @NonNull Class<T> baseEventFromKind) {
//    Constructor<T> constructor;
//    try {
//      constructor = baseEventFromKind.getConstructor(GenericEventRecord.class);
//      return constructor.newInstance(genericEventRecord);
//    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
//      throw new NostrException(e);
//    }
//  }
//}
