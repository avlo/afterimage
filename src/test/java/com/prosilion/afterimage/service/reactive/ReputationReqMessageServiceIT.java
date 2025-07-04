package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
import com.prosilion.afterimage.relay.AfterimageMeshRelayService;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.afterimage.util.TestSubscriber;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.GenericEventKindIF;
import com.prosilion.nostr.event.GenericEventKindTypeIF;
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
import com.prosilion.superconductor.dto.GenericEventKindTypeDto;
import com.prosilion.superconductor.service.event.EventService;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
  private final EventService eventService;
  private final Identity afterimageInstanceIdentity;

  @Autowired
  public ReputationReqMessageServiceIT(
      @NonNull EventService eventService,
      @NonNull AfterimageMeshRelayService afterimageMeshRelayService,
      @NonNull Identity afterimageInstanceIdentity) {
    this.afterimageMeshRelayService = afterimageMeshRelayService;
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.eventService = eventService;
  }

  @Test
  void testInvalidAfterImageReputationRequestEmptyFilters() {
    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    assertThrows(IllegalArgumentException.class, () -> afterimageMeshRelayService.send(
        new ReqMessage(
            Factory.generateRandomHex64String(),
            new Filters()),
        subscriber));
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
                    new IdentifierTag(AfterimageKindType.REPUTATION.getName())))),
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
    afterimageMeshRelayService.send(
        new ReqMessage(Factory.generateRandomHex64String(),
            new Filters(
                new KindFilter(Kind.BADGE_AWARD_EVENT))),
        subscriber);

    assertTrue(
        getNoticeMessage(
            subscriber.getItems())
            .getMessage()
            .contains(ReferencedPublicKeyFilter.FILTER_KEY));
  }

  @Test
  void testInvalidAfterImageReputationRequestMissingIdentifierTagFilter() throws IOException, NostrException {
    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        new ReqMessage(Factory.generateRandomHex64String(),
            new Filters(
                new KindFilter(Kind.BADGE_AWARD_EVENT),
                new ReferencedPublicKeyFilter(
                    new PubKeyTag(
                        Identity.generateRandomIdentity().getPublicKey())))),
        subscriber);

    assertTrue(
        getNoticeMessage(
            subscriber.getItems())
            .getMessage()
            .contains(IdentifierTagFilter.FILTER_KEY));
  }

  @Test
  void testInvalidAfterImageReputationRequestMissingReputationDTagFilter() throws IOException, NostrException {
    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        new ReqMessage(Factory.generateRandomHex64String(),
            new Filters(
                new KindFilter(Kind.BADGE_AWARD_EVENT),
                new ReferencedPublicKeyFilter(
                    new PubKeyTag(
                        Identity.generateRandomIdentity().getPublicKey())),
                new IdentifierTagFilter(
                    new IdentifierTag("invalid-uuid")))),
        subscriber);

    assertTrue(
        getNoticeMessage(
            subscriber.getItems())
            .getMessage()
            .contains(AfterimageKindType.REPUTATION.getName()));
  }

  @Test
  void testValidFilters() throws IOException, NostrException {
    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(
        new ReqMessage(Factory.generateRandomHex64String(),
            new Filters(
                new KindFilter(Kind.BADGE_AWARD_EVENT),
                new ReferencedPublicKeyFilter(
                    new PubKeyTag(
                        Identity.generateRandomIdentity().getPublicKey())),
                new IdentifierTagFilter(
                    new IdentifierTag(AfterimageKindType.REPUTATION.getName())))),
        subscriber);

    assertTrue(
        getGenericEvents(subscriber.getItems())
            .isEmpty());
  }

  @Test
  void testValidExistingEventThenAfterImageReputationRequest() throws IOException, NostrException, NoSuchAlgorithmException {
    final Identity authorIdentity = Identity.generateRandomIdentity();
    final PublicKey authorPublicKey = authorIdentity.getPublicKey();
    final PublicKey upvotedUserPubKey = Identity.generateRandomIdentity().getPublicKey();
    log.info("authorIdentity: {}", authorPublicKey);
    log.info("upvotedUserPubKey: {}", upvotedUserPubKey);

    GenericEventKindTypeIF upvoteEvent =
        new GenericEventKindTypeDto(
            new BadgeAwardUpvoteEvent(
                authorIdentity,
                upvotedUserPubKey),
            AfterimageKindType.UPVOTE)
            .convertBaseEventToGenericEventKindTypeIF();

//    simulate vote event received from SC
    eventService.processIncomingEvent(new EventMessage(upvoteEvent));

//    submit Req for above event to Aimg
    ReqMessage reputationReqMessage = new ReqMessage(
        Factory.generateRandomHex64String(),
        new Filters(
            new KindFilter(
                Kind.BADGE_AWARD_EVENT),
            new ReferencedPublicKeyFilter(
                new PubKeyTag(
                    upvotedUserPubKey)),
            new IdentifierTagFilter(
                new IdentifierTag(
                    AfterimageKindType.REPUTATION.getName()))));

    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(reputationReqMessage, subscriber);

    log.debug("retrieved afterimage events:");
    List<BaseMessage> items = subscriber.getItems();
    List<GenericEventKindIF> afterimageEvents = getGenericEvents(items);
    log.debug("  {}", items);
//    assertEquals(afterimageEvents.getFirst().getId(), upvoteEvent.getId());
    assertEquals(afterimageEvents.getFirst().getPublicKey(), afterimageInstanceIdentity.getPublicKey());
    AddressTag reputationAddressTag = Filterable.getTypeSpecificTags(AddressTag.class, afterimageEvents.getFirst()).getFirst();

    assertEquals(Kind.BADGE_DEFINITION_EVENT, reputationAddressTag.getKind());
    assertEquals(afterimageInstanceIdentity.getPublicKey(), reputationAddressTag.getPublicKey());
    assertEquals(AfterimageKindType.REPUTATION.getName(), reputationAddressTag.getIdentifierTag().getUuid());
  }

  private static NoticeMessage getNoticeMessage(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(NoticeMessage.class::isInstance)
        .map(NoticeMessage.class::cast).findFirst().orElseThrow(AssertionError::new);
  }

  private static List<GenericEventKindIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
  }
}
