package com.prosilion.afterimage.relay;

import com.prosilion.afterimage.event.ReputationEvent;
import com.prosilion.afterimage.util.ReputationCalculator;
import com.prosilion.superconductor.service.event.EventServiceIF;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import nostr.event.BaseTag;
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
  private final Identity aImgIdentity;

  @Autowired
  public AfterimageEventService(
      @NonNull EventServiceIF<T> eventService,
      @NonNull EventEntityService<T> eventEntityService,
      @NonNull Identity aImgIdentity) {
    this.eventService = eventService;
    this.eventEntityService = eventEntityService;
    this.aImgIdentity = aImgIdentity;
  }

  public <U extends EventMessage> void processIncomingEvent(@NonNull U eventMessage) {
    T event = (T) eventMessage.getEvent();

//    *********
//    save incoming 2112/Vote event prior to reputation scoring 
//    normally here would call
//        eventService.processIncomingEvent(eventMessage);
//    but using eventEntityService directly to not trigger subscriber listener event handling
//    *********    
    eventEntityService.saveEventEntity(event);

//    create reputation event    
    eventService.processIncomingEvent(
        new EventMessage(
            createReputationEvent(
                aImgIdentity,
                ReputationCalculator.calculateReputation(
                    eventEntityService
                        .getEventsByPublicKey(event.getPubKey()).stream().filter(e ->
                            e.getKind().equals(Kind.VOTE.getValue()))
                        .map(voteEvent -> voteEvent.getTags().stream()
                            .filter(VoteTag.class::isInstance)
                            .map(VoteTag.class::cast).toList())
                        .flatMap(Collection::stream).toList()),
//    non-parameterized replaceable variant:
//      ["a", <kind integer>:<32-bytes lowercase hex of a pubkey>:]
                new AddressTag(
                    Kind.REPUTATION.getValue(),
                    event.getPubKey()
//    or potentially/optionally parameterized replaceable:
//      ["a", <kind integer>:<32-bytes lowercase hex of a pubkey>:<d tag value>]
//    by uncommenting below line
//        , new IdentifierTag(String.format("REPUTATION_UUID-%s", reputationScore))
                ))));
  }

  private T createReputationEvent(@NonNull Identity identity, @NonNull Integer score, @NonNull BaseTag tag) {
    return createReputationEvent(identity, score, List.of(tag));
  }

  private T createReputationEvent(@NonNull Identity identity, @NonNull Integer score, @NonNull List<BaseTag> tags) {
    T t = (T) new ReputationEvent(
        identity.getPublicKey(),
        tags,
        score.toString());
    identity.sign(t);
    return t;
  }
}
