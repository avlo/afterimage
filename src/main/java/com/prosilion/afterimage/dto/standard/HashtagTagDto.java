package com.prosilion.afterimage.dto.standard;

import com.prosilion.afterimage.dto.AbstractTagDto;
import com.prosilion.afterimage.entity.standard.HashtagTagEntity;
import lombok.NonNull;
import nostr.event.tag.HashtagTag;

public class HashtagTagDto implements AbstractTagDto {
  private final HashtagTag hashtagTag;

  public HashtagTagDto(@NonNull HashtagTag hashtagTag) {
    this.hashtagTag = hashtagTag;
  }

  @Override
  public String getCode() {
    return hashtagTag.getCode();
  }

  @Override
  public HashtagTagEntity convertDtoToEntity() {
    return new HashtagTagEntity(hashtagTag);
  }
}
