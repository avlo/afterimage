//package com.prosilion.afterimage.config.eventaux;
//
//import com.prosilion.nostr.event.EventIF;
//import com.prosilion.nostr.event.GenericEventRecord;
//import com.prosilion.nostr.event.internal.Relay;
//import com.prosilion.superconductor.base.cache.CacheServiceIF;
//import java.util.Optional;
//import java.util.function.BiFunction;
//import lombok.NonNull;
//
//public interface EventAuxPluginIF {
//  BiFunction<CacheServiceIF, EventIF, Optional<GenericEventRecord>> eventAlreadyExistsFxn = (cacheServiceIF, eventIF) -> cacheServiceIF.getEventByEventId(eventIF.getId());
//
//  Optional<GenericEventRecord> processIncomingEvent(@NonNull EventIF var1, @NonNull Relay var2);
//}
