package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

import static com.prosilion.afterimage.config.MultiContainerTestConfig.AFTERIMAGE_APP_TWO;
import static com.prosilion.afterimage.config.MultiContainerTestConfig.SUPERCONDUCTOR_AFTERIMAGE;

@Slf4j
public class AbstractDockerRelayIT extends AbstractIT {
  protected final String superconductorRelayUrl;
  protected final String afterimageRelayUrlTwo;

  public AbstractDockerRelayIT(
     Identity afterimageInstanceIdentity,
     String superconductorRelayUrl,
     String afterimageRelayUrlTwo) throws ParseException, InterruptedException {
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

    submitRelayEvent(badgeAwardUpvoteEvent, superconductorRelayUrl);
    TimeUnit.MILLISECONDS.sleep(1000);

//  AIMG section		
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

  @Override
  protected BaseEvent createSearchRelaysListEventMessage() {
    return new SearchRelaysListEvent(
       Identity.generateRandomIdentity(),
       new RelaysTag(new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555")),
       "Search Relays List sent from aImg IT 5556");
  }
}
