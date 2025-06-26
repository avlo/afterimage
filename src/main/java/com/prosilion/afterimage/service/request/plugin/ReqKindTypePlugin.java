package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.user.Identity;
import lombok.Getter;
import lombok.NonNull;

public abstract class ReqKindTypePlugin implements ReqKindPlugin {
  @Getter
  private final Identity aImgIdentity;

  public ReqKindTypePlugin(@NonNull Identity aImgIdentity) {
    this.aImgIdentity = aImgIdentity;
  }

  public abstract KindTypeIF getKindType();
}
