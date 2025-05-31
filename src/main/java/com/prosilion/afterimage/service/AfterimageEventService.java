package com.prosilion.afterimage.service;

import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.ReputationCalculator;
import com.prosilion.superconductor.service.event.EventServiceIF;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import java.util.Collection;
import lombok.NonNull;
import nostr.event.Kind;
import nostr.event.impl.GenericEvent;
import nostr.event.message.EventMessage;
import nostr.event.tag.AddressTag;
import nostr.event.tag.VoteTag;
import nostr.id.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class AfterimageEventService<T extends GenericEvent> implements EventServiceIF<T> {
  private final EventServiceIF<T> eventService;
  private final EventEntityService<T> eventEntityService;
  private final Identity identity;

  @Autowired
  public AfterimageEventService(
      @NonNull EventServiceIF<T> eventService,
      @NonNull EventEntityService<T> eventEntityService,
      @NonNull Identity identity) {
    this.eventService = eventService;
    this.eventEntityService = eventEntityService;
    this.identity = identity;
  }

  public <U extends EventMessage> void processIncomingEvent(@NonNull U eventMessage) {
    T event = (T) eventMessage.getEvent();

//    *********
//    save incoming 2112/Vote event prior to scoring it
//    normally here would call
//        eventService.processIncomingEvent(eventMessage);
//    but trying using eventEntityService directly to not trigger subscriber listener event handling
//    *********    
    eventEntityService.saveEventEntity(event);

//    create new reputation event, non-parameterized replaceable variant:
//          ["a", <kind integer>:<32-bytes lowercase hex of a pubkey>:, <recommended relay URL, optional>]
//    or potentially/optionally parameterized replaceable:
//          ["a", <kind integer>:<32-bytes lowercase hex of a pubkey>:<d tag value>, <recommended relay URL, optional>]

    eventService.processIncomingEvent(
        new EventMessage(
            Factory.createReputationEvent(
                identity,
                ReputationCalculator.calculateReputation(
                    eventEntityService
                        .getEventsByPublicKey(event.getPubKey()).stream().filter(e ->
                            e.getKind().equals(Kind.VOTE.getValue()))
                        .map(voteEvent -> voteEvent.getTags().stream()
                            .filter(VoteTag.class::isInstance)
                            .map(VoteTag.class::cast).toList())
                        .flatMap(Collection::stream).toList()),
                new AddressTag(
                    Kind.REPUTATION.getValue(),
                    event.getPubKey()
//        , new IdentifierTag(String.format("REPUTATION_UUID-%s", reputationScore))
                ))));
  }
}
