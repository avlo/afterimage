package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.type.NonPublishingEventKindPlugin;
import com.prosilion.superconductor.lib.redis.service.RedisCacheServiceIF;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class AImg30000ReputationSetsFromAImgTypePlugin extends NonPublishingEventKindPlugin {
  private final Identity aImgIdentity;
  private final RedisCacheServiceIF redisCacheServiceIF;

  public AImg30000ReputationSetsFromAImgTypePlugin(
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull RedisCacheServiceIF redisCacheServiceIF,
      @NonNull Identity aImgIdentity) {
    super(eventKindPlugin);
    this.aImgIdentity = aImgIdentity;
    this.redisCacheServiceIF = redisCacheServiceIF;
  }

  @Override
  public void processIncomingEvent(@NonNull EventIF relayDiscoveryEvent) {
    log.debug("RelayDiscoveryEventTypePlugin processing incoming Kind.RELAY_DISCOVERY 30166 : [{}]", relayDiscoveryEvent);
/*{
  "kind": 30166,
  "tags": [
    ["d","ws://aimg.url:port"], <------  d-tag URI must be at index[1]
    // ["n", "clearnet"],
    ["T", "PublicOutbox" ]
    ["T", "Broadcast" ]
    ["vote", "VOTE_RECIP_1_PUBKEY", "vote_n_event_id", INTEGER_VOTE, "vote's SC source ws://sc.url:port"],
    ["vote", "VOTE_RECIP_1_PUBKEY", "vote_n+1_event_id", INTEGER_VOTE, "vote's SC source ws://sc.url:port"],
    ["vote", "VOTE_RECIP_1_PUBKEY", "vote_n+m_event_id", INTEGER_VOTE, "vote's SC source ws://sc.url:port"],
    ...
    ["vote", "VOTE_RECIP_2_PUBKEY", "vote_n_event_id", INTEGER_VOTE, "vote's SC source ws://sc.url:port"],
    ["vote", "VOTE_RECIP_2_PUBKEY", "vote_n+1_event_id", INTEGER_VOTE, "vote's SC source ws://sc.url:port"],
    ["vote", "VOTE_RECIP_2_PUBKEY", "vote_n+m_event_id", INTEGER_VOTE, "vote's SC source ws://sc.url:port"],
    ...
  ]}*/

//	if aImg already has 30166 EVENT containing same dTag (aka, relay_url), that means we already have that relay_url (and all it's votes) registered, so return (do nothing)
    IdentifierTag inomingIdentifierTag = Filterable.getTypeSpecificTags(IdentifierTag.class, relayDiscoveryEvent).getFirst();
    List<IdentifierTag> existingIdentifierTags = redisCacheServiceIF
        .getByKind(Kind.RELAY_DISCOVERY)
        .stream().map(kind30166Event ->
            Filterable.getTypeSpecificTags(IdentifierTag.class, kind30166Event))
        .findFirst().orElseThrow();

//     a1. if existing 30166 event with same dTag exists, return (do nothing, we already have that aImg relay)
    if (existingIdentifierTags.contains(inomingIdentifierTag))
      return;

//	otherwise, aImg doesn't yet have 30166 EVENT containing same dTag, so:
//	 	 for each unique aTag's pubKey
    Map<String, List<String>> pubKeyVotesMap = Filterable.getTypeSpecificTags(
            AddressTag.class, relayDiscoveryEvent).stream()
        .collect(
            Collectors.toMap(addressTag -> addressTag.getPublicKey().toHexString(),
                addressTag -> Collections.singletonList(Optional.ofNullable(addressTag.getIdentifierTag()).orElseThrow().getUuid())));

    //	   	3a. do REPUTATION calculation
    Map<String, String> pubKeyReputationMap = pubKeyVotesMap.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, entry ->
            entry.getValue().stream().reduce((s, s2) -> s + s2).orElseThrow()));


//	   	3b. save REPUTATION EVENT
//	 	 	3a. save 30166 event
//	 	 	3d. submit VOTE ReqMessage Filter(Kind.VOTE, Pubkey) to dTag    

  }

  @Override
  public Kind getKind() {
    return Kind.RELAY_DISCOVERY; // 30166
  }
}
