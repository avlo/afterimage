//package com.prosilion.afterimage.service;
//
//import com.prosilion.afterimage.event.BadgeAwardUpvoteEvent;
//import com.prosilion.afterimage.util.Factory;
//import com.prosilion.afterimage.util.TestSubscriber;
//import com.prosilion.nostr.enums.Kind;
//import com.prosilion.nostr.event.BadgeDefinitionEvent;
//import com.prosilion.nostr.event.EventIF;
//import com.prosilion.nostr.filter.Filters;
//import com.prosilion.nostr.filter.event.KindFilter;
//import com.prosilion.nostr.filter.tag.IdentifierTagFilter;
//import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
//import com.prosilion.nostr.message.BaseMessage;
//import com.prosilion.nostr.message.EventMessage;
//import com.prosilion.nostr.message.OkMessage;
//import com.prosilion.nostr.message.ReqMessage;
//import com.prosilion.nostr.tag.PubKeyTag;
//import com.prosilion.nostr.user.Identity;
//import com.prosilion.nostr.user.PublicKey;
//import com.prosilion.subdivisions.client.reactive.ReactiveEventPublisher;
//import com.prosilion.subdivisions.client.reactive.ReactiveRelaySubscriptionsManager;
//import com.prosilion.superconductor.base.service.event.service.GenericEventKindTypeIF;
//import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
//import com.prosilion.superconductor.lib.redis.dto.GenericDocumentKindTypeDto;
//import io.github.tobi.laa.spring.boot.embedded.redis.RedisFlushAll;
//import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone;
//import java.io.IOException;
//import java.security.NoSuchAlgorithmException;
//import java.util.List;
//import lombok.extern.slf4j.Slf4j;
//import org.jetbrains.annotations.NotNull;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
//import org.springframework.lang.NonNull;
//import org.springframework.test.context.ActiveProfiles;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@Slf4j
//@EmbeddedRedisStandalone
//@RedisFlushAll(mode = RedisFlushAll.Mode.AFTER_CLASS)
//@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
//@ActiveProfiles("test")
//class SuperconductorMeshIT {
//  private final ReactiveRelaySubscriptionsManager aImgReqClient;
//  private final ReactiveEventPublisher scEventClient;
//  private final BadgeDefinitionEvent upvoteBadgeDefinitionEvent;
//  private final BadgeDefinitionEvent reputationBadgeDefinitionEvent;
//
//  @Autowired
//  SuperconductorMeshIT(
//      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl,
//      @NonNull @Value("${afterimage-superconductor.relay.url}") String superconductorRelayUri,
//      @NonNull BadgeDefinitionEvent upvoteBadgeDefinitionEvent,
//      @NonNull BadgeDefinitionEvent reputationBadgeDefinitionEvent) {
//    log.debug("superconductorRelayUri: {}", superconductorRelayUri);
//    this.scEventClient = new ReactiveEventPublisher(superconductorRelayUri);
//    this.aImgReqClient = new ReactiveRelaySubscriptionsManager(afterimageRelayUrl);
//    this.upvoteBadgeDefinitionEvent = upvoteBadgeDefinitionEvent;
//    this.reputationBadgeDefinitionEvent = reputationBadgeDefinitionEvent;
//  }
//
//  @Test
//  void testSuperconductorEventThenAfterimageReq() throws IOException, NoSuchAlgorithmException {
//    Identity authorIdentity = Identity.generateRandomIdentity();
//    PublicKey upvotedUser = Identity.generateRandomIdentity().getPublicKey();
//
//    GenericEventKindTypeIF upvoteDto = createUpvoteDto(authorIdentity, upvotedUser);
//
////  lone SC publisher submits a vote Event to superconductor
//    TestSubscriber<OkMessage> voter = new TestSubscriber<>();
//    scEventClient.send(new EventMessage(upvoteDto), voter);
//    List<OkMessage> irrelevantLoneScVoteReturnedMessages = voter.getItems();
//    assertEquals(true, irrelevantLoneScVoteReturnedMessages.getFirst().getFlag());
//
//    /*
//     *  window when Aimg discovers and processes above vote event
//     */
//
////  lone Aimg subscriber submits reputation request of same PubKey to Aimg
//    TestSubscriber<BaseMessage> reputationSubscriber = new TestSubscriber<>();
//
//    aImgReqClient.send(
//        getReputationReqMessage(upvotedUser),
//        reputationSubscriber);
//
//    log.debug("superconductor events:");
//    List<BaseMessage> returnedBaseMessages = reputationSubscriber.getItems();
//    List<EventIF> afterImageReturnReputation = getGenericEvents(returnedBaseMessages);
//
//    assertEquals(1, afterImageReturnReputation.size());
//  }
//
//  private @NotNull ReqMessage getReputationReqMessage(PublicKey upvotedUserPubKey) {
//    return new ReqMessage(
//        Factory.generateRandomHex64String(),
//        new Filters(
//            new KindFilter(Kind.BADGE_AWARD_EVENT),
//            new ReferencedPublicKeyFilter(
//                new PubKeyTag(
//                    upvotedUserPubKey)),
//            new IdentifierTagFilter(
//                reputationBadgeDefinitionEvent.getIdentifierTag())));
//  }
//
//  private static List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
//    return returnedBaseMessages.stream()
//        .filter(EventMessage.class::isInstance)
//        .map(EventMessage.class::cast)
//        .map(EventMessage::getEvent)
//        .toList();
//  }
//
//  Filters getFilters() {
//    log.debug("SuperConductorRelayEnlistmentEventTypePlugin getFilters() of Kind.BADGE_AWARD_EVENT");
//    return new Filters(new KindFilter(Kind.BADGE_AWARD_EVENT));
//  }
//
//  private GenericEventKindTypeIF createUpvoteDto(Identity authorIdentity, PublicKey upvotedUser) throws NoSuchAlgorithmException {
//    return new GenericDocumentKindTypeDto(
//        new BadgeAwardUpvoteEvent(
//            authorIdentity,
//            upvotedUser,
//            upvoteBadgeDefinitionEvent),
//        SuperconductorKindType.UPVOTE).convertBaseEventToGenericEventKindTypeIF();
//  }
//}
