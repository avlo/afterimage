package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosilion.afterimage.config.MultiContainerTestConfig;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.util.Factory;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.RelaySetsEvent;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.filter.tag.AddressTagFilter;
import com.prosilion.nostr.filter.tag.ExternalIdentityTagFilter;
import com.prosilion.nostr.filter.tag.ReferencedPublicKeyFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.OkMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.nostr.util.Util;
import com.prosilion.subdivisions.client.reactive.NostrEventPublisher;
import com.prosilion.subdivisions.client.reactive.NostrSingleRelayRequestService;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static com.prosilion.afterimage.enums.AfterimageKindType.BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(MultiContainerTestConfig.class)
public class FollowSetsIT {
  /**
   * definitionsCreatorIdentity:   02d49b23e02985a760e8bc2f5ee86a3089569806f5f6a670fba3317568d14262
   * <p>
   * voteSubmitterIdentity:        611eda70943b4f67d1674068f5c86cedbdc3438bb41245b129a6311e4f308295
   * <p>
   * voteReceierIdentity:          985a5b9ea911bb8f9d9dca82c03f776d68fdc452b774295a874423a0fa5e8879
   */

  public static final String REPUTATION = "TEST_REPUTATION";
  public static final String AWARD_UNIT_UPVOTE = "TEST_UNIT_UPVOTE";

  public static final String PLUS_ONE_FORMULA = "+1";

  private final IdentifierTag reputationIdentifierTag = new IdentifierTag(REPUTATION);
  private final IdentifierTag upvoteIdentifierTag = new IdentifierTag(AWARD_UNIT_UPVOTE);

  private final Identity definitionsCreatorIdentity = // Identity.generateRandomIdentity();
      Identity.create("bbb4585483196998204846989544737603523651520600328805626488477202");

  private final BadgeDefinitionReputationEvent badgeDefinitionReputationEventPlusOneFormula;
  private final Relay afterimageRelay;

  private final Identity afterimageIdentity;
  private final Identity voteRecipientIdentity;

  private final String afterimageRelayUrl2;

  private final String superconductorRelayUrl;
  private final Relay superconductorRelayUrlTag;
  Duration requestTimeoutDuration;

  @Autowired
  public FollowSetsIT(
      @NonNull Identity afterimageInstanceIdentity,
      @NonNull String afterimageRelayUrl,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl,
      @NonNull @Value("${afterimage.relay.url.two}") String afterimageRelayUrl2,
      Duration requestTimeoutDuration) throws ParseException, IOException, InterruptedException {
    this.afterimageRelayUrl2 = afterimageRelayUrl2;
    this.afterimageIdentity = afterimageInstanceIdentity;
    this.afterimageRelay = new Relay(afterimageRelayUrl);
    this.superconductorRelayUrl = superconductorRelayUrl;
    this.requestTimeoutDuration = requestTimeoutDuration;
//    this.superconductorRelayUrlTag = new Relay(superconductorRelayUrl);
//    this.superconductorRelayUrlTag = new Relay("ws://superconductor-afterimage:5555");
    this.superconductorRelayUrlTag = new Relay(superconductorRelayUrl.replace("localhost", "0.0.0.0"));

    Identity voteSubmitterIdentity = Identity.create("aaa4585483196998204846989544737603523651520600328805626488477202");
    voteRecipientIdentity = // Identity.generateRandomIdentity();
        Identity.create("ccc4585483196998204846989544737603523651520600328805626488477202");

    BadgeDefinitionGenericEvent awardUpvoteDefinitionEvent = new BadgeDefinitionGenericEvent(
        definitionsCreatorIdentity,
        upvoteIdentifierTag,
        superconductorRelayUrlTag,
        String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", definitionsCreatorIdentity.getPublicKey()));

    FormulaEvent plusOneFormulaEvent = new FormulaEvent(
        definitionsCreatorIdentity,
        upvoteIdentifierTag,
        superconductorRelayUrlTag,
        awardUpvoteDefinitionEvent,
        PLUS_ONE_FORMULA);

    badgeDefinitionReputationEventPlusOneFormula = new BadgeDefinitionReputationEvent(
        definitionsCreatorIdentity,
        reputationIdentifierTag,
        superconductorRelayUrlTag,
        AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
        plusOneFormulaEvent);

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent =
        new BadgeAwardGenericEvent<>(
            voteSubmitterIdentity,
            voteRecipientIdentity.getPublicKey(),
            superconductorRelayUrlTag,
            badgeDefinitionReputationEventPlusOneFormula,
            String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", voteRecipientIdentity.getPublicKey()));

////    reminder : do lsports for relay2's docker internal url ...
    log.debug("1of4 - sendEventToSuperconductor(awardUpvoteDefinitionEvent)");
    sendEventToSuperconductor(awardUpvoteDefinitionEvent);
//    cacheService.save(awardUpvoteDefinitionEvent);
    TimeUnit.MILLISECONDS.sleep(100);

    log.debug("2of4 - sendEventToSuperconductor(plusOneFormulaEvent)");
////    ... req'd by events' validation / relay  processing calls    
    sendEventToSuperconductor(plusOneFormulaEvent);
//    cacheService.save(plusOneFormulaEvent);
    TimeUnit.MILLISECONDS.sleep(100);

    log.debug("3of4 - sendEventToSuperconductor(badgeDefinitionReputationEventPlusOneFormula)");
////    ... req'd by events' validation / relay  processing calls    
    sendEventToSuperconductor(badgeDefinitionReputationEventPlusOneFormula);
//    cacheService.save(badgeDefinitionReputationEventPlusOneFormula);
    TimeUnit.MILLISECONDS.sleep(100);

    log.debug("4of4 - sendEventToSuperconductor(badgeAwardUpvoteEvent)");
////    ... req'd by events' validation / relay  processing calls    
//    cacheService.save(badgeAwardUpvoteEvent);
//    sendEventToAimg2(badgeAwardUpvoteEvent);
    sendEventToSuperconductor(badgeAwardUpvoteEvent);
  }

  @Test
  void testFollowSetsEvent() throws IOException, InterruptedException {
    Identity searchRelaysListEventSubmitterIdentity = Identity.generateRandomIdentity();
    log.debug("\n\n\nSearch Relays List sent from aImg IT 5556...");
    SearchRelaysListEvent searchRelaysListEvent = new SearchRelaysListEvent(
        searchRelaysListEventSubmitterIdentity,
        new RelaysTag(superconductorRelayUrlTag),
        "Search Relays List sent from aImg IT 5556");

//  send awareness to aImg5557 of SC 5555    
    new NostrEventPublisher(afterimageRelayUrl2)
        .send(
            new EventMessage(
                searchRelaysListEvent));
    TimeUnit.MILLISECONDS.sleep(2000);
    log.debug("...done.\n\n\ncalling  getAimgRepReqResult(afterimageRelayUrl2).getFirst().getContent()...");

// ******* bonus/aside: validate afterimageRelayUrl2 reputation created for voteRecipientIdentity 
    List<EventIF> aimgRepReqResult = getAimgRepReqResult(afterimageRelayUrl2);
    assertEquals("1", aimgRepReqResult.getFirst().getContent());
    log.debug("...done\n\n\n");
// ******* end bonus

////  then send awareness to 5556 of RELAY SETS 5557
//    new AfterimageMeshRelayService(afterimageRelay.getUrl())
//        .send(
//            new EventMessage(
////  ...announcing 5557               
//                createRelaysSetsEventMessage(afterimageRelayUrl2)),
//            new TestSubscriber<>());
//
//    TimeUnit.MILLISECONDS.sleep(2000);
//
////      sent REP request to 5556
//    List<EventIF> returnedReqGenericEvents_2 = getAimgRepReqResult(afterimageRelay.getUrl());
//    assertEquals("1", returnedReqGenericEvents_2.getFirst().getContent());
  }

  private List<EventIF> getAimgRepReqResult(final String afterimageRelayUrl) throws JsonProcessingException, InterruptedException {
    fail();
    final NostrSingleRelayRequestService afterimageRepRequestClient = new NostrSingleRelayRequestService(afterimageRelayUrl);
    List<BaseMessage> items_3 = afterimageRepRequestClient.send(
        createAfterImageReqMessage(
            Factory.generateRandomHex64String(),
            voteRecipientIdentity.getPublicKey(),
            definitionsCreatorIdentity.getPublicKey()));

    TimeUnit.MILLISECONDS.sleep(100);

    log.debug("afterimage returned superconductor events:");
    log.debug("  {}", items_3);

    List<EventIF> returnedReqGenericEvents_2 = getGenericEvents(items_3);
    return returnedReqGenericEvents_2;
  }

  private ReqMessage createAfterImageReqMessage(
      String subscriberId,
      PublicKey upvotedUserPublicKey,
      PublicKey badgeCreatorPublicKey) throws JsonProcessingException {
    System.out.println("333333333333333333333");
    System.out.println("333333333333333333333");
    ExternalIdentityTagFilter externalIdentityTagFilter = new ExternalIdentityTagFilter(BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG);
    ReqMessage reqMessageWithStuff = new ReqMessage(
        subscriberId,
        new Filters(
            new KindFilter(
                Kind.BADGE_AWARD_EVENT),
//            new IdentifierTagFilter(reputationIdentifierTag),
            new AddressTagFilter(
                new AddressTag(
                    Kind.BADGE_DEFINITION_EVENT,
                    badgeCreatorPublicKey,
                    reputationIdentifierTag)),
            new ReferencedPublicKeyFilter(
                new PubKeyTag(
                    upvotedUserPublicKey)),
            externalIdentityTagFilter));

    ReqMessage reqMessage = reqMessageWithStuff;
    log.debug(Util.prettyFormatJson(reqMessage.encode()));
    System.out.println("333333333333333333333");
    System.out.println("333333333333333333333");
    return reqMessage;
  }

  private BaseEvent createRelaysSetsEventMessage(String uri) {
    return new RelaySetsEvent(
        afterimageIdentity,
        new RelaysTag(new Relay(uri)),
        "RELAY SETS awareness send to 5556 of 5557");
  }

  private List<EventIF> getGenericEvents(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
        .filter(EventMessage.class::isInstance)
        .map(EventMessage.class::cast)
        .map(EventMessage::getEvent)
        .toList();
  }

  private void sendEventToAimg2(BaseEvent baseEvent) throws IOException, InterruptedException {
    final String RED_BOLD_BRIGHT = "\033[1;91m";
    final String GREEN_BOLD = "\033[1;32m";
    final String RESET = "\033[0m";
    String greenFont = GREEN_BOLD + "%s" + RESET;
    String redFont = RED_BOLD_BRIGHT + "%s" + RESET;

    OkMessage upvoteDefinitionOkMessageItems = new NostrEventPublisher(afterimageRelayUrl2).send(
        new EventMessage(baseEvent));
    Boolean flag = upvoteDefinitionOkMessageItems.getFlag();
    log.debug("\n\n  ***********  OKMessage received from sendEventToAimgRelay2? [{}] ************\n",
        String.format(flag ? greenFont : redFont, flag.toString().toUpperCase()));
//    assertEquals(true, upvoteDefinitionOkMessageItems.getFirst().getFlag());
//    new AfterimageMeshRelayService(afterimageRelayUrl2).closeSocket();
    TimeUnit.MILLISECONDS.sleep(250);
  }

  private void sendEventToSuperconductor(BaseEvent baseEvent) throws IOException, InterruptedException {
    final String RED_BOLD_BRIGHT = "\033[1;91m";
    final String GREEN_BOLD = "\033[1;32m";
    final String RESET = "\033[0m";
    String greenFont = GREEN_BOLD + "%s" + RESET;
    String redFont = RED_BOLD_BRIGHT + "%s" + RESET;

    OkMessage scReturnedOkMessage = new NostrEventPublisher(superconductorRelayUrl).send(
        new EventMessage(baseEvent));
    Boolean flag = scReturnedOkMessage.getFlag();
    log.debug("\n\n  ***********  OKMessage received from sendEventToAimgRelay2? [{}] ************\n",
        String.format(flag ? greenFont : redFont, flag.toString().toUpperCase()));
//    assertEquals(true, upvoteDefinitionOkMessageItems.getFirst().getFlag());
//    new AfterimageMeshRelayService(afterimageRelayUrl2).closeSocket();
    TimeUnit.MILLISECONDS.sleep(250);
  }
}
