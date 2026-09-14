package com.prosilion.afterimage.service.reactive.abstracts;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.afterimage.util.EventAttributesMap;
import com.prosilion.nostr.event.BadgeAwardCanonicalEvent;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.SearchRelaysListEvent;
import com.prosilion.nostr.event.curated.BadgeDefinitionReputationEvent;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.RelaysTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.util.Util;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

import static com.prosilion.afterimage.config.ContainerTestConfig.AFTERIMAGE_APP_TWO;

@Slf4j
public class AbstractDockerRelayIT extends AbstractWithRelaysTagIT {
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

    BadgeAwardCanonicalEvent badgeAwardUpvoteEvent = createUpvoteEventForCanonicalRecipient(superconductorRelay);

    EventIF simulateIncomingUpvoteEvent = submitSCEvent(
       badgeAwardUpvoteEvent,
       superconductorRelayUrl,
       upvoteAndOrDownvoteEventFilter);
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
  protected BadgeDefinitionReputationEvent createBadgeDefinitionReputationEvent() {
    return new BadgeDefinitionReputationEvent(
       repDefnCreator,
       afterimageInstanceIdentity.getPublicKey(),
       reputationIdentifierTag,
       AfterimageKindType.BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG,
       new Relay("ws://" + AFTERIMAGE_APP_TWO + ":5556"),
       EventAttributesMap.asEventList(this.dbCuratedFormulaEventList));
  }
}
