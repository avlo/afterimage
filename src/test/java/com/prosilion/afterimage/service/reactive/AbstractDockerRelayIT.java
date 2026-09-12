package com.prosilion.afterimage.service.reactive;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.event.BadgeAwardGenericEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.curated.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.curated.CuratedFormulaEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.ReferenceTag;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.util.Util;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

import static com.prosilion.afterimage.config.ContainerTestConfig.AFTERIMAGE_APP_TWO;

@Slf4j
public class AbstractDockerRelayIT extends AbstractIT {
  protected final String superconductorRelayUrl;
  protected final String afterimageRelayUrlTwo;

  private static final Relay badgeAwardEventRelay = new Relay("ws://superconductor-app:5555");
  private static final Relay badgeDefinitionEventRelay = new Relay("ws://superconductor-app:5555");

  public AbstractDockerRelayIT(
     Identity afterimageInstanceIdentity,
     String superconductorRelayUrl,
     String afterimageRelayUrlTwo) throws InterruptedException {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrlTwo);
    this.superconductorRelayUrl = superconductorRelayUrl;
    this.afterimageRelayUrlTwo = afterimageRelayUrlTwo;

    BadgeAwardGenericEvent<BadgeDefinitionGenericEvent> badgeAwardUpvoteEvent =
       new BadgeAwardGenericEvent<>(
          submitter,
          recipient.getPublicKey(),
          awardUpvoteDefinitionEvent,
          String.format("badgeAwardUpvoteEvent, vote recipient PublicKey: [%s]", recipient.getPublicKey()),
          new Relay(superconductorRelayUrl));

    submitRelayEvent(badgeAwardUpvoteEvent, superconductorRelayUrl);
    TimeUnit.MILLISECONDS.sleep(1000);

//  AIMG section
    Util.debug(log, "AbstractDockerRelayITs - watch SC for incoming request search relays within next", "5 seconds", true, 'A');
    TimeUnit.MILLISECONDS.sleep(5_000);
    submitRelayEvent(
       createSearchRelaysListEventMessageAbstractDockerRelay(),
       afterimageRelayUrlTwo);
    TimeUnit.MILLISECONDS.sleep(1000);
  }

  protected BaseEvent createSearchRelaysListEventMessageAbstractDockerRelay() {
    return new SearchRelaysListEvent(
       Identity.generateRandomIdentity(),
       new RelaysTag(new Relay(superconductorRelayUrl)),
       "Search Relays List sent from aImg IT 5556");
  }

  @Override
  protected BadgeDefinitionGenericEvent createBadgeAwardUpvoteDefinitionEvent() {
    return new BadgeDefinitionGenericEvent(
       upvoteAndOrDownvoteDefnCreator,
       upvoteIdentifierTag,
       String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteAndOrDownvoteDefnCreator.getPublicKey()),
       new Relay(superconductorRelayUrl));
  }

  protected BadgeDefinitionGenericEvent createBadgeAwardDownvoteDefinitionEvent() {
    return new BadgeDefinitionGenericEvent(
       upvoteAndOrDownvoteDefnCreator,
       downvoteIdentifierTag,
       String.format("awardUpvoteDefinitionEvent, definition creator PublicKey: [%s]", upvoteAndOrDownvoteDefnCreator.getPublicKey()),
       new Relay(superconductorRelayUrl));
  }

  @Override
  protected CuratedFormulaEvent createCuratedFormulaPlusOneEvent() {
    return new CuratedFormulaEvent(
       afterimageInstanceIdentity,
       new FormulaEvent(
          formulaCreator,
          formulaUpvoteIdentifierTag,
          awardUpvoteDefinitionEvent,
          PLUS_ONE_FORMULA,
          new Relay(superconductorRelayUrl)),
       new ReferenceTag(afterimageRelayUrl),
       new Relay(superconductorRelayUrl));
  }

  @Override
  protected CuratedFormulaEvent createCuratedFormulaMinusOneEvent() {
    return new CuratedFormulaEvent(
       afterimageInstanceIdentity,
       new FormulaEvent(
          formulaCreator,
          formulaDownvoteIdentifierTag,
          awardDownvoteDefinitionEvent,
          MINUS_ONE_FORMULA,
          new Relay(superconductorRelayUrl)),
       new ReferenceTag(afterimageRelayUrl),
       new Relay(superconductorRelayUrl));
  }

  @Override
  protected BadgeDefinitionReputationEvent createBadgeDefinitionReputationEvent() {
    return new BadgeDefinitionReputationEvent(
       repDefnCreator,
       afterimageInstanceIdentity.getPublicKey(),
       reputationIdentifierTag,
       AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       new Relay("ws://" + AFTERIMAGE_APP_TWO + ":5556"),
       plusOneCuratedFormulaEvent, minusOneCuratedFormulaEvent);
  }
}
