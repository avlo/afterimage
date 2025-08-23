package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.user.Identity;
import lombok.Getter;
import lombok.NonNull;

public abstract class ReqKindTypePlugin implements ReqKindTypePluginIF {
  @Getter
  private final Identity aImgIdentity;
  @Getter
  private final KindTypeIF kindType;

  public ReqKindTypePlugin(@NonNull KindTypeIF kindType, @NonNull Identity aImgIdentity) {
    this.kindType = kindType;
    this.aImgIdentity = aImgIdentity;
  }
}
