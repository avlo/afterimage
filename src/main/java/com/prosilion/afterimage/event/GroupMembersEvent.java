package com.prosilion.afterimage.event;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.NostrException;
import com.prosilion.nostr.event.AddressableEvent;
import com.prosilion.nostr.tag.BaseTag;
import com.prosilion.nostr.user.Identity;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.NonNull;

public class GroupMembersEvent extends AddressableEvent {
  public GroupMembersEvent(@NonNull Identity identity, @NonNull Kind kind, @NonNull List<BaseTag> baseTags, @NonNull String content) throws NostrException, NoSuchAlgorithmException {
    super(identity, kind, baseTags, content);
  }
}
