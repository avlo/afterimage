package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.enums.AfterimageKindType;
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
import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import com.prosilion.superconductor.base.util.EmptyFiltersException;
import com.prosilion.superconductor.lib.redis.dto.GenericDocumentKindTypeDto;
import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
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
@EmbeddedRedisStandalone
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class ReputationReqMessageServiceIT {
  private final AfterimageMeshRelayService afterimageMeshRelayService;
  private final EventServiceIF eventService;
  private final Identity afterimageInstanceIdentity;
  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;

  @Autowired
  public ReputationReqMessageServiceIT(
      @NonNull EventServiceIF eventService,
      @NonNull AfterimageMeshRelayService afterimageMeshRelayService,
      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent,
      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent,
      @NonNull Identity afterimageInstanceIdentity) {
    this.afterimageMeshRelayService = afterimageMeshRelayService;
    this.afterimageInstanceIdentity = afterimageInstanceIdentity;
    this.eventService = eventService;
    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;
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
  void testValidExistingEventThenAfterImageReputationRequest() throws IOException, NostrException, NoSuchAlgorithmException {
    final Identity authorIdentity = Identity.generateRandomIdentity();
    final PublicKey upvotedUserPubKey = Identity.generateRandomIdentity().getPublicKey();
    log.info("authorIdentity: {}", authorIdentity.getPublicKey());
    log.info("upvotedUserPubKey: {}", upvotedUserPubKey);

    GenericEventKindTypeIF upvoteEvent =
        new GenericDocumentKindTypeDto(
            new BadgeAwardUpvoteEvent(
                authorIdentity,
                upvotedUserPubKey,
                upvoteBadgeDefinitionEvent),
            SuperconductorKindType.UPVOTE)
            .convertBaseEventToGenericEventKindTypeIF();

    eventService.processIncomingEvent(new EventMessage(upvoteEvent));

//    submit Req for above event to Aimg

    ReqMessage reputationReqMessage = getReputationReqMessage(upvotedUserPubKey);

    TestSubscriber<BaseMessage> subscriber = new TestSubscriber<>();
    afterimageMeshRelayService.send(reputationReqMessage, subscriber);

    log.debug("retrieved afterimage events:");
    List<BaseMessage> items = subscriber.getItems();
    List<EventIF> afterimageEvents = getGenericEvents(items);
    log.debug("  {}", items);
//    assertEquals(afterimageEvents.getFirst().getId(), upvoteEvent.getId());
    assertEquals(afterimageEvents.getFirst().getPublicKey(), afterimageInstanceIdentity.getPublicKey());
    AddressTag reputationAddressTag = Filterable.getTypeSpecificTags(AddressTag.class, afterimageEvents.getFirst()).getFirst();

    assertEquals(Kind.BADGE_DEFINITION_EVENT, reputationAddressTag.getKind());
    assertEquals(afterimageInstanceIdentity.getPublicKey(), reputationAddressTag.getPublicKey());
    assertEquals(AfterimageKindType.REPUTATION.getName(), Optional.ofNullable(reputationAddressTag.getIdentifierTag()).orElseThrow().getUuid());
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

  private static NoticeMessage getNoticeMessage(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(NoticeMessage.class::isInstance)
        .map(NoticeMessage.class::cast).findFirst().orElseThrow(AssertionError::new);
  }

  private static List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
  }
}
