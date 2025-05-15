package com.prosilion.afterimage.service.reactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.util.AfterimageRelayReactiveClient;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.superconductor.service.event.EventService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.BaseMessage;
import nostr.event.BaseTag;
import nostr.event.filter.Filters;
import nostr.event.filter.ReferencedPublicKeyFilter;
import nostr.event.filter.VoteTagFilter;
import nostr.event.impl.GenericEvent;
import nostr.event.message.EventMessage;
import nostr.event.message.NoticeMessage;
import nostr.event.message.ReqMessage;
import nostr.event.tag.PubKeyTag;
import nostr.event.tag.VoteTag;
import nostr.id.Identity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.prosilion.afterimage.service.reactive.SuperconductorEventThenAfterimageReqIT.getGenericEvents;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class ReputationReqMessageServiceIT {
  private final AfterimageRelayReactiveClient afterimageRelayReactiveClient;
  private final EventService<GenericEvent> eventService;

  final static int KIND = 2112;
  private final VoteTag voteTag = new VoteTag(1);

  @Autowired
  public ReputationReqMessageServiceIT(
      @NonNull EventService<GenericEvent> eventService,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUri) {
    log.debug("afterimageRelayUri: {}", afterimageRelayUri);

    this.afterimageRelayReactiveClient = new AfterimageRelayReactiveClient(afterimageRelayUri);
    this.eventService = eventService;
  }

  @Test
  void testInvalidAfterImageReputationRequestMissingAuthorTagFilter() throws IOException {
    final VoteTag voteTag = new VoteTag(1);
    final String subscriberId = Factory.generateRandomHex64String();

    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageRelayReactiveClient.send(
        new ReqMessage(subscriberId,
            new Filters(
                new VoteTagFilter<>(voteTag))),
        subscriber);

    NoticeMessage noticeMessage = getNoticeMessage(subscriber.getItems()).orElseThrow(AssertionError::new);
    assertTrue(noticeMessage.getMessage().contains("does not contain required Author Tag and Vote tag"));
  }

  @Test
  void testInvalidAfterImageReputationRequestMissingVoteTagFilter() throws IOException {
    final Identity authorIdentity = Factory.createNewIdentity();
    final String subscriberId = Factory.generateRandomHex64String();

    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageRelayReactiveClient.send(
        new ReqMessage(subscriberId,
            new Filters(
                new ReferencedPublicKeyFilter<>(new PubKeyTag(authorIdentity.getPublicKey())))),
        subscriber);

    NoticeMessage noticeMessage = getNoticeMessage(subscriber.getItems()).orElseThrow(AssertionError::new);
    assertTrue(noticeMessage.getMessage().contains("does not contain required Author Tag and Vote tag"));
  }

  @Test
  void testValidExistingEventThenAfterImageReputationRequest() throws IOException {
    final Identity identity = Factory.createNewIdentity();
    final String CONTENT = Factory.lorumIpsum(ReputationReqMessageServiceIT.class);
    final Identity authorIdentity = Factory.createNewIdentity();

    List<BaseTag> tags = new ArrayList<>();
    tags.add(voteTag);
    tags.add(new PubKeyTag(authorIdentity.getPublicKey()));

    GenericEvent event = Factory.createTextNoteEvent(identity, tags, CONTENT);
    event.setKind(KIND);
    identity.sign(event);

    eventService.processIncomingEvent(new EventMessage(event));

    final String subscriberId = Factory.generateRandomHex64String();
//    submit Req for above event to superconductor

    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageRelayReactiveClient.send(
        new ReqMessage(subscriberId,
            new Filters(
                new ReferencedPublicKeyFilter<>(new PubKeyTag(authorIdentity.getPublicKey())),
                new VoteTagFilter<>(voteTag))),
        subscriber);

    log.debug("retrieved afterimage events:");
    List<BaseMessage> items = subscriber.getItems();
    List<GenericEvent> afterimageEvents = getGenericEvents(items);
    log.debug("  {}", items);
    assertEquals(afterimageEvents.getFirst().getId(), event.getId());
    assertEquals(afterimageEvents.getFirst().getPubKey(), event.getPubKey());
    assertEquals(KIND, (int) afterimageEvents.getFirst().getKind());
  }

  public static <T extends BaseMessage> Optional<NoticeMessage> getNoticeMessage(List<T> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(NoticeMessage.class::isInstance)
        .map(NoticeMessage.class::cast)
        .reduce((noticeMessage, noticeMessage2) -> noticeMessage);
  }
}
