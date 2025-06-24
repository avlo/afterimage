package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.relay.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.enums.NostrException;
import com.prosilion.nostr.event.GenericEventKindIF;
import com.prosilion.nostr.event.GenericEventKindTypeIF;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.NoticeMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.dto.GenericEventKindTypeDto;
import com.prosilion.superconductor.service.event.EventService;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class ReputationReqMessageServiceIT {
  private final AfterimageMeshRelayService afterimageMeshRelayService;
  private final EventService eventService;
  private final Identity afterimageInstanceIdentity;
  private final List<KindTypeIF> kindTypes;

  @Autowired
  public ReputationReqMessageServiceIT(
      @NonNull EventService eventService,
      @NonNull AfterimageMeshRelayService afterimageMeshRelayService,
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull List<KindTypeIF> kindTypes) {
    this.afterimageMeshRelayService = afterimageMeshRelayService;
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.eventService = eventService;
    this.kindTypes = kindTypes;
  }

  @Test
  void testInvalidAfterImageReputationRequestMissingAuthorTagFilter() throws IOException, NostrException {
    final String subscriberId = Factory.generateRandomHex64String();

    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        new ReqMessage(subscriberId,
            new Filters(
                new KindFilter(Kind.BADGE_AWARD_EVENT))),
        subscriber);

    NoticeMessage noticeMessage = getNoticeMessage(subscriber.getItems()).orElseThrow(AssertionError::new);
    assertTrue(noticeMessage.getMessage().contains(String.format("does not contain required [%s] tag", ReferencedPublicKeyFilter.FILTER_KEY)));
  }

  @Test
  void testInvalidAfterImageReputationRequestMissingVoteTagFilter() throws IOException, NostrException {
    final Identity authorIdentity = Identity.generateRandomIdentity();
    final String subscriberId = Factory.generateRandomHex64String();

    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        new ReqMessage(subscriberId,
            new Filters(
                new ReferencedPublicKeyFilter(new PubKeyTag(authorIdentity.getPublicKey())))),
        subscriber);

    NoticeMessage noticeMessage = getNoticeMessage(subscriber.getItems()).orElseThrow(AssertionError::new);
    assertTrue(noticeMessage.getMessage().contains(String.format("does not contain required [%s] tag", Kind.BADGE_AWARD_EVENT.getName())));
  }

  @Test
  void testValidExistingEventThenAfterImageReputationRequest() throws IOException, NostrException, NoSuchAlgorithmException {
    final Identity upvotedUser = Identity.generateRandomIdentity();
    final Identity authorIdentity = afterimageInstanceIdentity;

    BadgeAwardUpvoteEvent event = new BadgeAwardUpvoteEvent(authorIdentity, upvotedUser.getPublicKey());
    GenericEventKindTypeIF genericEventKindIF = new GenericEventKindTypeDto(event, kindTypes).convertBaseEventToGenericEventKindTypeIF();

    eventService.processIncomingEvent(new EventMessage(genericEventKindIF));

    final String subscriberId = Factory.generateRandomHex64String();
//    submit Req for above event to superconductor

    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        new ReqMessage(subscriberId,
            new Filters(
                new ReferencedPublicKeyFilter(new PubKeyTag(authorIdentity.getPublicKey())),
                new KindFilter(Kind.BADGE_AWARD_EVENT))),
        subscriber);

    log.debug("retrieved afterimage events:");
    List<BaseMessage> items = subscriber.getItems();
    List<GenericEventKindIF> afterimageEvents = getGenericEvents(items);
    log.debug("  {}", items);
    assertEquals(afterimageEvents.getFirst().getId(), event.getId());
    assertEquals(afterimageEvents.getFirst().getPublicKey(), event.getPublicKey());
    assertEquals(Kind.BADGE_AWARD_EVENT, afterimageEvents.getFirst().getKind());
  }

  public static <T extends BaseMessage> Optional<NoticeMessage> getNoticeMessage(List<T> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(NoticeMessage.class::isInstance)
        .map(NoticeMessage.class::cast)
        .reduce((noticeMessage, noticeMessage2) -> noticeMessage);
  }

  public static List<GenericEventKindIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
  }
}
