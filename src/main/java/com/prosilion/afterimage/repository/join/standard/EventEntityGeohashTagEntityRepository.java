package com.prosilion.afterimage.repository.join.standard;

import com.prosilion.afterimage.entity.join.standard.EventEntityGeohashTagEntity;
import com.prosilion.afterimage.repository.join.EventEntityAbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventEntityGeohashTagEntityRepository<T extends EventEntityGeohashTagEntity> extends EventEntityAbstractTagEntityRepository<T> {
  default String getCode() {
    return "g";
  }
}
