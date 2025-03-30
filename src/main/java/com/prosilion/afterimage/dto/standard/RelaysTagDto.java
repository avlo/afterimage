package com.prosilion.afterimage.dto.standard;

import com.prosilion.afterimage.dto.AbstractTagDto;
import com.prosilion.afterimage.entity.standard.RelaysTagEntity;
import lombok.Getter;
import lombok.NonNull;
import nostr.event.tag.RelaysTag;

@Getter
public class RelaysTagDto implements AbstractTagDto {
  private final RelaysTag relaysTag;

  public RelaysTagDto(@NonNull RelaysTag relaysTag) {
    this.relaysTag = relaysTag;
  }

  @Override
  public String getCode() {
    return relaysTag.getCode();
  }

  @Override
  public RelaysTagEntity convertDtoToEntity() {
    return new RelaysTagEntity(relaysTag);
  }
}
