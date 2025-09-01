package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.nostr.user.Identity;
import lombok.Getter;
import lombok.NonNull;

public abstract class ReqKindTypePlugin implements ReqKindTypePluginIF {
  @Getter
  private final Identity aImgIdentity;

  public ReqKindTypePlugin(@NonNull Identity aImgIdentity) {
    this.aImgIdentity = aImgIdentity;
  }
}
