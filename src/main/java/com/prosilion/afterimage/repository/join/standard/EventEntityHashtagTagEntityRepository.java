package com.prosilion.afterimage.repository.join.standard;

import com.prosilion.afterimage.entity.join.standard.EventEntityHashtagTagEntity;
import com.prosilion.afterimage.repository.join.EventEntityAbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventEntityHashtagTagEntityRepository<T extends EventEntityHashtagTagEntity> extends EventEntityAbstractTagEntityRepository<T> {
  default String getCode() {
    return "t";
  }
}
