package com.prosilion.afterimage.service.reactive.abstracts;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.util.EventAttributesMap;
import com.prosilion.nostr.event.BadgeAwardCanonicalEvent;
import com.prosilion.nostr.event.BadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FormulaEvent;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.curated.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.util.Util;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

import static com.prosilion.afterimage.config.ContainerTestConfig.AFTERIMAGE_APP_TWO;
import static com.prosilion.afterimage.config.ContainerTestConfig.SUPERCONDUCTOR_AFTERIMAGE;

@Slf4j
public class AbstractDockerRelayIT extends AbstractWithRelaysTagIT {
  protected static final Relay SUPERCONDUCTOR_DOCKER_RELAY = new Relay("ws://" + SUPERCONDUCTOR_AFTERIMAGE + ":5555");
  protected static final Relay AFTERIMAGE_TWO_RELAY = new Relay("ws://" + AFTERIMAGE_APP_TWO + ":5556");

  protected final String superconductorRelayUrl;
  protected final String afterimageRelayUrlTwo;

  public AbstractDockerRelayIT(
     Identity afterimageInstanceIdentity,
     String superconductorRelayUrl,
     String afterimageRelayUrlTwo,
     CacheServiceIF cacheServiceIF) throws InterruptedException {
    super(afterimageInstanceIdentity, superconductorRelayUrl, afterimageRelayUrlTwo, cacheServiceIF);
    this.superconductorRelayUrl = superconductorRelayUrl;
    this.afterimageRelayUrlTwo = afterimageRelayUrlTwo;

    BadgeAwardCanonicalEvent badgeAwardUpvoteEvent = createUpvoteEventForCanonicalRecipient(SUPERCONDUCTOR_DOCKER_RELAY);

    submitSCEvent(
       badgeAwardUpvoteEvent,
       superconductorRelayUrl,
       upvoteAndOrDownvoteEventFilter);
    TimeUnit.MILLISECONDS.sleep(1000);

//  AIMG section
    Util.debug(log, "AbstractDockerRelayITs - watch SC for incoming request search relays within next", "5 seconds", true, 'A');
    TimeUnit.MILLISECONDS.sleep(1000);
    submitRelayEvent(
       createSearchRelaysListEventMessageAbstractDockerRelay(),
       afterimageRelayUrlTwo);
    TimeUnit.MILLISECONDS.sleep(1000);
  }

  protected BaseEvent createSearchRelaysListEventMessageAbstractDockerRelay() {
    return new SearchRelaysListEvent(
       Identity.generateRandomIdentity(),
       new RelaysTag(SUPERCONDUCTOR_DOCKER_RELAY),
       "Search Relays List sent from aImg IT 5556");
  }

  @Override
  protected BadgeDefinitionReputationEvent createBadgeDefinitionReputationEvent() {
    return new BadgeDefinitionReputationEvent(
       repDefnCreator,
       afterimageInstanceIdentity.getPublicKey(),
       reputationIdentifierTag,
       AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       AFTERIMAGE_TWO_RELAY,
       EventAttributesMap.asEventList(this.dbCuratedFormulaEventList));
  }

  protected List<EventAttributesMap<BadgeDefinitionGenericEvent>> createBadgeDefinitionGenericEventList() {
    return
       EventAttributesMap.asEventAttributesMap(List.of(
          createBadgeAwardUpvoteDefinitionEvent(SUPERCONDUCTOR_DOCKER_RELAY)
          ,
          createBadgeAwardDownvoteDefinitionEvent(SUPERCONDUCTOR_DOCKER_RELAY)
       ));
  }

  protected FormulaEvent createPlusOneFormulaEvent() {
    return new FormulaEvent(
       formulaCreator,
       formulaUpvoteIdentifierTag,
       EventAttributesMap.getFirstByIdentifierTag(
          this.badgeDefinitionGenericEventList, upvoteIdentifierTag),
       PLUS_ONE_FORMULA,
       SUPERCONDUCTOR_DOCKER_RELAY);
  }

  protected FormulaEvent createMinusOneFormulaEvent() {
    return new FormulaEvent(
       formulaCreator,
       formulaDownvoteIdentifierTag,
       EventAttributesMap.getFirstByIdentifierTag(
          this.badgeDefinitionGenericEventList, downvoteIdentifierTag),
       MINUS_ONE_FORMULA,
       SUPERCONDUCTOR_DOCKER_RELAY);
  }
}
