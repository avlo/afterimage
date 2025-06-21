package com.prosilion.afterimage.request;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.user.Identity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReputationReqKindTypePlugin extends ReqKindTypePlugin {

  @Autowired
  public ReputationReqKindTypePlugin(@NonNull Identity aImgIdentity) {
    super(aImgIdentity);
    log.debug("loaded ReputationReqKindTypePlugin bean");
  }

  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;  // 2113 REQ is an incoming reputation req from a person   
  }
}
