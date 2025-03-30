package com.prosilion.afterimage.dto.standard;

import com.prosilion.afterimage.dto.AbstractTagDto;
import com.prosilion.afterimage.entity.standard.IdentifierTagEntity;
import lombok.NonNull;
import nostr.event.tag.IdentifierTag;

public class IdentifierTagDto implements AbstractTagDto {
  private final IdentifierTag identifierTag;

  public IdentifierTagDto(@NonNull IdentifierTag identifierTag) {
    this.identifierTag = identifierTag;
  }

  @Override
  public String getCode() {
    return identifierTag.getCode();
  }

  @Override
  public IdentifierTagEntity convertDtoToEntity() {
    return new IdentifierTagEntity(identifierTag);
  }
}
