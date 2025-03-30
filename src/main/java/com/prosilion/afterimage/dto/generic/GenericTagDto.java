package com.prosilion.afterimage.dto.generic;

import com.prosilion.afterimage.entity.generic.GenericTagEntity;

import java.util.List;

public record GenericTagDto(String code, List<ElementAttributeDto> atts) {
  public GenericTagEntity convertDtoToEntity() {
    return new GenericTagEntity(code);
  }
}
