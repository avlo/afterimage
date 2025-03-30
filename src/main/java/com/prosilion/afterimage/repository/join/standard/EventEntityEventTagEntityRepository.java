package com.prosilion.afterimage.repository.join.standard;

import com.prosilion.afterimage.entity.join.standard.EventEntityEventTagEntity;
import com.prosilion.afterimage.repository.join.EventEntityAbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventEntityEventTagEntityRepository<T extends EventEntityEventTagEntity> extends EventEntityAbstractTagEntityRepository<T> {
  default String getCode() {
    return "e";
  }
}
