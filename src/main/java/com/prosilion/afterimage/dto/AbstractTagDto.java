package com.prosilion.afterimage.dto;

import com.prosilion.afterimage.entity.AbstractTagEntity;

public interface AbstractTagDto {
  String getCode();

  AbstractTagEntity convertDtoToEntity();
}
