//package com.prosilion.afterimage.service;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.prosilion.afterimage.AfterimageMeshRelayService;
//import com.prosilion.afterimage.Subscriber;
//import com.prosilion.nostr.event.GenericEventId;
//import com.prosilion.nostr.event.GenericEventRecord;
//import com.prosilion.nostr.filter.Filters;
//import com.prosilion.nostr.filter.event.EventFilter;
//import com.prosilion.nostr.message.BaseMessage;
//import com.prosilion.nostr.message.EventMessage;
//import com.prosilion.nostr.message.ReqMessage;
//import com.prosilion.nostr.tag.EventTag;
//import com.prosilion.superconductor.base.service.CacheDereferenceEventTagServiceIF;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//import java.util.function.Supplier;
//import lombok.SneakyThrows;
//import org.springframework.lang.NonNull;
//
//public class CacheDereferenceEventTagServiceClient implements CacheDereferenceEventTagServiceIF {
//  private final CacheDereferenceEventTagServiceIF cacheDereferenceEventTagServiceIF;
//
//  public CacheDereferenceEventTagServiceClient(@NonNull CacheDereferenceEventTagServiceIF cacheDereferenceEventTagServiceIF) {
//    this.cacheDereferenceEventTagServiceIF = cacheDereferenceEventTagServiceIF;
//  }
//
//  @SneakyThrows
//  @Override
//  public Optional<GenericEventRecord> getEvent(@NonNull EventTag eventTag) {
//    Optional<GenericEventRecord> localEvent = cacheDereferenceEventTagServiceIF
//        .getEvent(
//            eventTag);
//    Optional<GenericEventRecord> genericEventRecord = localEvent
//        .or(
//            supplier(eventTag));
//    return genericEventRecord;
//  }
//
//  private Supplier<Optional<GenericEventRecord>> supplier(EventTag eventTag) throws JsonProcessingException {
//    Subscriber<BaseMessage> eventSubscriber = new Subscriber<>();
//    AfterimageMeshRelayService relayService = new AfterimageMeshRelayService(eventTag.getRecommendedRelayUrl());
//    relayService.send(
//        createSuperconductorReqMessage(generateRandomHex64String(), eventTag),
//        eventSubscriber);
//    List<GenericEventRecord> returnedAfterImageEvents = getGenericEvents(eventSubscriber.getItems());
//
//    Supplier<Optional<GenericEventRecord>> supplier = () -> Optional.ofNullable(returnedAfterImageEvents.getFirst());
//    return supplier;
//  }
//
//  private ReqMessage createSuperconductorReqMessage(String subscriberId, EventTag eventTag) {
//    ReqMessage reqMessage = new ReqMessage(subscriberId,
//        new Filters(
//            new EventFilter(new GenericEventId(eventTag.getIdEvent()))));
//    return reqMessage;
//  }
//
//  private List<GenericEventRecord> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
//    List<GenericEventRecord> list = returnedBaseMessages.stream()
//        .filter(EventMessage.class::isInstance)
//        .map(EventMessage.class::cast)
//        .map(EventMessage::getEvent)
//        .map(eventIF -> new GenericEventRecord(
//            eventIF.getId(),
//            eventIF.getPublicKey(),
//            eventIF.getCreatedAt(),
//            eventIF.getKind(),
//            eventIF.getTags(),
//            eventIF.getContent(),
//            eventIF.getSignature()))
//        .toList();
//    return list;
//  }
//
//  public static String generateRandomHex64String() {
//    return UUID.randomUUID().toString().concat(UUID.randomUUID().toString()).replaceAll("[^A-Za-z0-9]", "");
//  }
//}
