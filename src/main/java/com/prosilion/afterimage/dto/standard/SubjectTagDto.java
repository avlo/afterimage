package com.prosilion.afterimage.dto.standard;

import com.prosilion.afterimage.dto.AbstractTagDto;
import com.prosilion.afterimage.entity.standard.SubjectTagEntity;
import lombok.NonNull;
import nostr.event.tag.SubjectTag;

public class SubjectTagDto implements AbstractTagDto {
  private final SubjectTag subjectTag;

  public SubjectTagDto(@NonNull SubjectTag subjectTag) {
    this.subjectTag = subjectTag;
  }

  @Override
  public String getCode() {
    return subjectTag.getCode();
  }

  @Override
  public SubjectTagEntity convertDtoToEntity() {
    return new SubjectTagEntity(limit80(subjectTag.getSubject()));
  }

  private String limit80(String string) {
    return string.chars().limit(80).collect(
        StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
  }
}
