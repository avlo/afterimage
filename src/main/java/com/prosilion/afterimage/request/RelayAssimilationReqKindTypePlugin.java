package com.prosilion.afterimage.request;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.event.Kind;
import nostr.id.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RelayAssimilationReqKindTypePlugin<T extends Kind> extends ReqKindTypePlugin<T> {

  @Autowired
  public RelayAssimilationReqKindTypePlugin(@NonNull Identity aImgIdentity) {
    super(aImgIdentity);
    log.debug("loaded RelayAssimilationReqKindTypePlugin bean");
  }

  @Override
  public T getKind() {
    return (T) Kind.GROUP_ADMINS;
  }
}
