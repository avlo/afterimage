package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.MultiContainerTestConfig;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.subdivisions.client.RequestSubscriber;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static com.prosilion.afterimage.config.MultiContainerTestConfig.AFTERIMAGE_APP_TWO;
import static com.prosilion.afterimage.config.MultiContainerTestConfig.SUPERCONDUCTOR_AFTERIMAGE;

/**
 * test name "SearchRelaysListRelaySets" means:
 * BadgeDefinitionReputationEvent and SearchRelaysListEvent for docker (5557) aImg relay
 * note: varies from {@link SearchRelaysListSameRelayIT}, which is 5556
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(MultiContainerTestConfig.class)
public class SearchRelaysListDockerRelayIT extends AbstractIT {
  private final String superconductorRelayUrl;
  private final String afterimageRelayUrlTwo;

  @Autowired
  public SearchRelaysListDockerRelayIT(
     @NonNull Identity afterimageInstanceIdentity,
     @NonNull @Value("${afterimage.relay.url.two}") String afterimageRelayUrlTwo,
     @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl) throws ParseException, InterruptedException {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrlTwo);
    this.superconductorRelayUrl = superconductorRelayUrl;
    this.afterimageRelayUrlTwo = afterimageRelayUrlTwo;

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent =
       new BadgeAwardGenericEvent<>(
          submitter,
          recipient.getPublicKey(),
          new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555"),
          awardUpvoteDefinitionEvent,
          String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", recipient.getPublicKey()));

    log.debug("2of5 - sendEventToSuperconductor(badgeAwardUpvoteEvent)");
    submitRelayEvent(badgeAwardUpvoteEvent, superconductorRelayUrl);
    TimeUnit.MILLISECONDS.sleep(1000);

//  AIMG section		
    log.debug("5of5 - submitRelayEvent(createSearchRelaysListEventMessage(),afterimageRelayUrlTwo)");
    submitRelayEvent(
       createSearchRelaysListEventMessage(),
       afterimageRelayUrlTwo);
    TimeUnit.MILLISECONDS.sleep(1000);
  }

  @Override
  protected BadgeDefinitionGenericEvent createBadgeAwardUpvoteDefinitionEvent() {
    return new BadgeDefinitionGenericEvent(
       upvoteDefnCreator,
       upvoteIdentifierTag,
       new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555"),
       String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteDefnCreator.getPublicKey()));
  }

  protected BadgeDefinitionGenericEvent createBadgeAwardDownvoteDefinitionEvent() {
    return new BadgeDefinitionGenericEvent(
       upvoteDefnCreator,
       downvoteIdentifierTag,
       new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555"),
       String.format("awardDownvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteDefnCreator.getPublicKey()));
  }

  @Override
  protected FormulaEvent createFormulaUpvoteEvent() throws ParseException {
    return new FormulaEvent(
       formulaCreator,
       formulaUpvoteIdentifierTag,
       new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555"),
       awardUpvoteDefinitionEvent,
       PLUS_ONE_FORMULA);
  }

  @Override
  protected FormulaEvent createFormulaDownvoteEvent() throws ParseException {
    return new FormulaEvent(
       formulaCreator,
       formulaDownvoteIdentifierTag,
       new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555"),
       awardDownvoteDefinitionEvent,
       MINUS_ONE_FORMULA);
  }

  @Override
  protected BadgeDefinitionReputationEvent createBadgeDefinitionReputationEvent() {
    return new BadgeDefinitionReputationEvent(
       repDefnCreator,
       submitter.getPublicKey(),
       reputationIdentifierTag,
       new Relay("ws://" + AFTERIMAGE_APP_TWO + ":5556"),
       AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       plusOneFormulaEvent);
  }

  @Test
  void testA_SuperconductorEventThenAfterimageReq() throws NostrException, InterruptedException {
// aImg_2 sanity check		
    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_A = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(upvoteDefnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()),
       afterimageRelayUrlTwo,
       aImg_2_EventSubscriber_A);

    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_A, 1, "1");

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent_2 =
       new BadgeAwardGenericEvent<>(
          submitter,
          recipient.getPublicKey(),
          new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555"),
          awardUpvoteDefinitionEvent,
          String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", recipient.getPublicKey()));

//  submit upvote event to SC
    submitRelayEvent(badgeAwardUpvoteEvent_2, superconductorRelayUrl);
    TimeUnit.MILLISECONDS.sleep(1500);

// aImg_2 sanity check		
    RequestSubscriber<BaseMessage> aImg_2_EventSubscriber_B = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(upvoteDefnCreator.getPublicKey(), new PubKeyTag(recipient.getPublicKey()), afterimageRelayUrlTwo, aImg_2_EventSubscriber_B);

    validateSpecificAfterimageRequestResults(aImg_2_EventSubscriber_B, 1, "2");
  }

  @Override
  protected BaseEvent createSearchRelaysListEventMessage() {
    return new SearchRelaysListEvent(
       Identity.generateRandomIdentity(),
       new RelaysTag(new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555")),
       "Search Relays List sent from aImg IT 5556");
  }
}
