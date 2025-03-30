package com.prosilion.afterimage.dto.standard;

import com.prosilion.afterimage.dto.AbstractTagDto;
import com.prosilion.afterimage.entity.standard.PubkeyTagEntity;
import lombok.NonNull;
import nostr.event.tag.PubKeyTag;

public class PubkeyTagDto implements AbstractTagDto {
  private final PubKeyTag pubKeyTag;

  public PubkeyTagDto(@NonNull PubKeyTag pubKeyTag) {
    this.pubKeyTag = pubKeyTag;
  }

  @Override
  public String getCode() {
    return pubKeyTag.getCode();
  }

  @Override
  public PubkeyTagEntity convertDtoToEntity() {
    return new PubkeyTagEntity(pubKeyTag);
  }
}
