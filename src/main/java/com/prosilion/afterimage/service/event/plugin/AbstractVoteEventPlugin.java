package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.afterimage.calculator.DynamicReputationCalculator;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.internal.EventTagAddressTagPair;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.base.service.event.type.NonPublishingEventKindTypePlugin;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import static com.prosilion.afterimage.enums.AfterimageKindType.UNIT_REPUTATION;

@Slf4j
// our SportsCar extends CarDecorator
public abstract class AbstractVoteEventPlugin extends NonPublishingEventKindTypePlugin {
  private final EventKindPluginIF afterimageFollowSetsEventPlugin;
  private final Identity aImgIdentity;

  public AbstractVoteEventPlugin(
      @NonNull EventKindTypePluginIF eventKindTypePluginIF,
      @NonNull EventKindPluginIF afterimageFollowSetsEventPlugin,
      @NonNull Identity aImgIdentity) {
    super(eventKindTypePluginIF);
    this.afterimageFollowSetsEventPlugin = afterimageFollowSetsEventPlugin;
    this.aImgIdentity = aImgIdentity;
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF voteEvent) {
    log.debug("{} processing incoming VOTE EVENT: [{}]", getClass().getSimpleName(), voteEvent);
    afterimageFollowSetsEventPlugin.processIncomingEvent(
        createFollowSetsEvent(
            Filterable.getTypeSpecificTags(PubKeyTag.class, voteEvent)
                .stream()
                .map(PubKeyTag::getPublicKey)
                .findFirst().orElseThrow(),
            new EventTagAddressTagPair(
                new EventTag(voteEvent.getId()),
                Filterable.getTypeSpecificTags(AddressTag.class, voteEvent)
                    .stream()
                    .findFirst().orElseThrow())));
  }

  @SneakyThrows
  private EventIF createFollowSetsEvent(
      @NonNull PublicKey voteReceiverPubkey,
      @NonNull EventTagAddressTagPair eventTagAddressTagPairs) {
    return new FollowSetsEvent(
        aImgIdentity,
        voteReceiverPubkey,
        new IdentifierTag(
// TODO CRITICAL: below needs replaced via proper reference chain, ex:
//   ["a", "30009:BADGE_AWARD_DEFN_UPVOTE_CREATOR_PUBKEY:UNIT_UPVOTE"]
//       where pubkey and uuid refers to formula event relevant fields to obtain event_id:
            /*
{
  "id": "BadgeDefinitionReputationFormulaEventId_1"  <------- obtain
  "pubkey": "REP_DEFN_CREATOR_PUBKEY",  <-------------------- matching pubkey
  "kind": 30078, // Kind.ARBITRARY_APP_DATA
  "tags": [
    [ "d", "UNIT_UPVOTE"                <--------------------- matching uuid
  "content": "+1",
}            
             */
// TODO: after which pubkey and eventId are then used to find a single badge definition event (30009)
//    for matching "e" tag, 
            /*
{
  "id": "BADGE_DEFINITION_REPUTATION_EVENT_ID",
  "pubkey": "REP_DEFN_CREATOR_PUBKEY",                    <------------------- matching pubkey
  "kind": 30009, // Kind.BADGE_DEFINITION_EVENT       
  "tags": [
    [ "d", "USER_DEFINED_REPUTATION_DEFINITION_NAME"],  //  UNIT_REPUTATION  <-------------- obtain
      below event tag classes are BadgeDefinitionReputationFormulaEvent instances
    [ "e", "BadgeDefinitionReputationFormulaEventId_1", "ws://aimg.url:port"]  <----- matching e tag
    ...
  ],
  "content": "afterimage reputation definition f(x)",
}            
             */
// TODO: after which USER_DEFINED_REPUTATION_DEFINITION_NAME is used to populate identifierTag on next line
            UNIT_REPUTATION.getName()),  
        List.of(eventTagAddressTagPairs),
//        TODO: replace content
        "Follow Sets Event Content: available for useful use").getGenericEventRecord();
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }
}
