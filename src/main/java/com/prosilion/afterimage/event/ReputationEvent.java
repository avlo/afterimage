package com.prosilion.afterimage.event;

import java.util.List;
import nostr.base.PublicKey;
import nostr.event.BaseTag;
import nostr.event.Kind;
import nostr.event.NIP01Event;

public class ReputationEvent extends NIP01Event {
  public ReputationEvent(PublicKey pubKey, List<BaseTag> tags, String content) {
    super(pubKey, Kind.REPUTATION, tags, content);
  }
}
