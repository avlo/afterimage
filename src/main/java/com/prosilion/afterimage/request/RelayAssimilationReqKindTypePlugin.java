package com.prosilion.afterimage.request;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.user.Identity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RelayAssimilationReqKindTypePlugin extends ReqKindTypePlugin {

  @Autowired
  public RelayAssimilationReqKindTypePlugin(@NonNull Identity aImgIdentity) {
    super(aImgIdentity);
    log.debug("loaded RelayAssimilationReqKindTypePlugin bean");
  }

  @Override
  public Kind getKind() {
    return Kind.GROUP_ADMINS; 
  }

  @Override
  public KindTypeIF getKindType() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
