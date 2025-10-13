package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.BadgeAwardDownvoteEvent;
import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.util.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeDefinitionEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.NoticeMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.EventServiceIF;
import com.prosilion.superconductor.base.util.EmptyFiltersException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class ReputationReqMessageServiceIT {
  private final AfterimageMeshRelayService afterimageMeshRelayService;
  private final EventServiceIF eventService;
  private final Identity afterimageInstanceIdentity;
  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
  private final BadgeDefinitionEvent downvoteBadgeDefinitionEvent;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;

  @Autowired
  public ReputationReqMessageServiceIT(
      @NonNull EventServiceIF eventService,
      @NonNull AfterimageMeshRelayService afterimageMeshRelayService,
      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent,
      @NonNull BadgeDefinitionEvent downvoteBadgeDefinitionEvent,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent,
      @NonNull Identity afterimageInstanceIdentity) {
    this.afterimageMeshRelayService = afterimageMeshRelayService;
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.eventService = eventService;
    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;
    this.downvoteBadgeDefinitionEvent = downvoteBadgeDefinitionEvent;
    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
  }

  @Test
  void testInvalidAfterImageReputationRequestEmptyFilters() {
    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    assertThrows(IllegalArgumentException.class, () -> afterimageMeshRelayService.send(
        new ReqMessage(
            Factory.generateRandomHex64String(),
            new Filters()),
        subscriber));
    log.debug("testInvalidAfterImageReputationRequestEmptyFilters");
  }

  @Test
  void testInvalidAfterImageRequestEmptyFiltersList() {
    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    assertThrows(IllegalArgumentException.class, () -> afterimageMeshRelayService.send(
        new ReqMessage(
            Factory.generateRandomHex64String(),
            new Filters(List.of())),
        subscriber));
  }

  @Test
  void testInvalidAfterImageReputationRequestMissingKindFilter() throws IOException, NostrException {
    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        new ReqMessage(Factory.generateRandomHex64String(),
            new Filters(
                new ReferencedPublicKeyFilter(
                    new PubKeyTag(Identity.generateRandomIdentity().getPublicKey())))),
        subscriber);

    assertTrue(
        getNoticeMessage(
            subscriber.getItems())
            .getMessage()
            .contains(
                KindFilter.FILTER_KEY));
  }

  @Test
  void testInvalidAfterImageReputationRequestOnlyIdentifierFilter() throws IOException, NostrException {
    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        new ReqMessage(Factory.generateRandomHex64String(),
            new Filters(
                new IdentifierTagFilter(
                    new IdentifierTag(AfterimageKindType.UNIT_REPUTATION.getName())))),
        subscriber);

    assertTrue(
        getNoticeMessage(
            subscriber.getItems())
            .getMessage()
            .contains(
                KindFilter.FILTER_KEY));
  }

  @Test
  void testInvalidAfterImageReputationRequestMissingAuthorTagFilter() throws IOException, NostrException {
    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    Filters filters = new Filters(new KindFilter(Kind.BADGE_AWARD_EVENT));

    afterimageMeshRelayService.send(
        new ReqMessage(Factory.generateRandomHex64String(),
            filters),
        subscriber);

    assertEquals(
        String.format(
            EmptyFiltersException.FILTERS_EXCEPTION, List.of(filters), "PubKeyTag"),
        getNoticeMessage(
            subscriber.getItems()).getMessage());
  }

  @Test
  void testInvalidAfterImageReputationHasIdentifierTagFilterInsteadOfAddressTagFilter() throws IOException, NostrException {
    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    Filters filters = new Filters(
        new KindFilter(Kind.BADGE_AWARD_EVENT),
        new ReferencedPublicKeyFilter(
            new PubKeyTag(
                Identity.generateRandomIdentity().getPublicKey())));

    afterimageMeshRelayService.send(
        new ReqMessage(Factory.generateRandomHex64String(),
            filters),
        subscriber);

    assertEquals(
        String.format(
            EmptyFiltersException.FILTERS_EXCEPTION, List.of(filters), "IdentifierTag"),
        getNoticeMessage(
            subscriber.getItems()).getMessage());
  }

  @Test
  void testValidFilters() throws IOException, NostrException {
    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        getReputationReqMessage(Identity.generateRandomIdentity().getPublicKey()),
        subscriber);

    assertTrue(
        getGenericEvents(subscriber.getItems())
            .isEmpty());
  }

  @Test
  void testValidExistingEventThenAfterImageReputationRequest() throws IOException, NostrException {
    final Identity authorIdentity = Identity.generateRandomIdentity();
    final PublicKey upvotedUserPubKey = Identity.generateRandomIdentity().getPublicKey();
    log.info("authorIdentity: {}", authorIdentity.getPublicKey());
    log.info("upvotedUserPubKey: {}", upvotedUserPubKey);

    BadgeAwardUpvoteEvent event_1 = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUserPubKey,
        upvoteBadgeDefinitionEvent);
    eventService.processIncomingEvent(new EventMessage(event_1));

//    submit Req for above event_1 to Aimg
    ReqMessage reputationReqMessage = getReputationReqMessage(upvotedUserPubKey);

    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(reputationReqMessage, subscriber);

    log.debug("retrieved afterimage events:");
    List<BaseMessage> items = subscriber.getItems();
    List<EventIF> afterimageEvents = getGenericEvents(items);
    log.debug("  {}", items);
//    assertEquals(afterimageEvents_3.getFirst().getId(), upvoteEvent.getId());
    assertEquals(afterimageEvents.getFirst().getPublicKey(), afterimageInstanceIdentity.getPublicKey());
    AddressTag reputationAddressTag = Filterable.getTypeSpecificTags(AddressTag.class, afterimageEvents.getFirst()).getFirst();

    assertEquals(Kind.BADGE_DEFINITION_EVENT, reputationAddressTag.getKind());
    assertEquals(afterimageInstanceIdentity.getPublicKey(), reputationAddressTag.getPublicKey());
    assertEquals(AfterimageKindType.UNIT_REPUTATION.getName(), Optional.ofNullable(reputationAddressTag.getIdentifierTag()).orElseThrow().getUuid());

    BadgeAwardUpvoteEvent event_2 = new BadgeAwardUpvoteEvent(
        authorIdentity,
        upvotedUserPubKey,
        upvoteBadgeDefinitionEvent);
    eventService.processIncomingEvent(new EventMessage(event_2));

    BadgeAwardDownvoteEvent event_3 = new BadgeAwardDownvoteEvent(
        authorIdentity,
        upvotedUserPubKey,
        downvoteBadgeDefinitionEvent);
    eventService.processIncomingEvent(new EventMessage(event_3));

    ReqMessage reputationReqMessage_3 = getReputationReqMessage(upvotedUserPubKey);
    TestSubscriber<BaseMessage> subscriber_3 = new TestSubscriber<>();
    afterimageMeshRelayService.send(reputationReqMessage_3, subscriber_3);

    log.debug("retrieved afterimage events:");
    List<BaseMessage> items_3 = subscriber_3.getItems();
    List<EventIF> afterimageEvents_3 = getGenericEvents(items_3);
    log.debug("  {}", items_3);
//    assertEquals(afterimageEvents_3.getFirst().getId(), upvoteEvent.getId());
    assertEquals(afterimageEvents_3.getFirst().getPublicKey(), afterimageInstanceIdentity.getPublicKey());
    AddressTag reputationAddressTag_3 = Filterable.getTypeSpecificTags(AddressTag.class, afterimageEvents_3.getFirst()).getFirst();

    assertEquals(Kind.BADGE_DEFINITION_EVENT, reputationAddressTag_3.getKind());
    assertEquals(afterimageInstanceIdentity.getPublicKey(), reputationAddressTag_3.getPublicKey());
    assertEquals(AfterimageKindType.UNIT_REPUTATION.getName(), Optional.ofNullable(reputationAddressTag_3.getIdentifierTag()).orElseThrow().getUuid());
  }

  private @NotNull ReqMessage getReputationReqMessage(PublicKey upvotedUserPubKey) {
    return new ReqMessage(
        Factory.generateRandomHex64String(),
        new Filters(
            new KindFilter(Kind.BADGE_AWARD_EVENT),
            new ReferencedPublicKeyFilter(
                new PubKeyTag(
                    upvotedUserPubKey)),
            new IdentifierTagFilter(
                reputationBadgeDefinitionEvent.getIdentifierTag())));
  }

  private NoticeMessage getNoticeMessage(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(NoticeMessage.class::isInstance)
        .map(NoticeMessage.class::cast).findFirst().orElseThrow(AssertionError::new);
  }

  private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
  }
}
