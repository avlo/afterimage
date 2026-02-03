//package com.prosilion.afterimage.service;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.prosilion.afterimage.AfterimageMeshRelayService;
//import com.prosilion.afterimage.Subscriber;
//import com.prosilion.nostr.event.GenericEventRecord;
//import com.prosilion.nostr.filter.Filters;
//import com.prosilion.nostr.filter.tag.AddressTagFilter;
//import com.prosilion.nostr.message.BaseMessage;
//import com.prosilion.nostr.message.EventMessage;
//import com.prosilion.nostr.message.ReqMessage;
//import com.prosilion.nostr.tag.AddressTag;
//import com.prosilion.superconductor.base.service.CacheDereferenceAddressTagServiceIF;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//import java.util.function.Supplier;
//import lombok.SneakyThrows;
//import org.springframework.lang.NonNull;
//
//public class CacheDereferenceAddressTagServiceClient implements CacheDereferenceAddressTagServiceIF {
//  private final CacheDereferenceAddressTagServiceIF cacheDereferenceAddressTagServiceIF;
//
//  public CacheDereferenceAddressTagServiceClient(@NonNull CacheDereferenceAddressTagServiceIF cacheDereferenceAddressTagServiceIF) {
//    this.cacheDereferenceAddressTagServiceIF = cacheDereferenceAddressTagServiceIF;
//  }
//
//  @SneakyThrows
//  @Override
//  public Optional<GenericEventRecord> getEvent(@NonNull AddressTag addressTag) {
//    Optional<GenericEventRecord> localEvent = cacheDereferenceAddressTagServiceIF
//        .getEvent(
//            addressTag);
//    Optional<GenericEventRecord> genericEventRecord = localEvent
//        .or(
//            supplier(addressTag));
//    return genericEventRecord;
//  }
//
//  private Supplier<Optional<GenericEventRecord>> supplier(AddressTag addressTag) throws JsonProcessingException {
//    Subscriber<BaseMessage> eventSubscriber = new Subscriber<>();
//    AfterimageMeshRelayService relayService = new AfterimageMeshRelayService(addressTag.getRelay().getUrl());
//    relayService.send(
//        createSuperconductorReqMessage(generateRandomHex64String(), addressTag),
//        eventSubscriber);
//    List<GenericEventRecord> returnedAfterImageEvents = getGenericEvents(eventSubscriber.getItems());
//
//    Supplier<Optional<GenericEventRecord>> supplier = () -> Optional.ofNullable(returnedAfterImageEvents.getFirst());
//    return supplier;
//  }
//
//  private ReqMessage createSuperconductorReqMessage(String subscriberId, AddressTag addressTag) {
//    ReqMessage reqMessage = new ReqMessage(subscriberId,
//        new Filters(
//            new AddressTagFilter(addressTag)));
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
