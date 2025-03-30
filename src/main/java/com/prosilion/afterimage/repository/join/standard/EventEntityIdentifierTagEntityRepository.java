package com.prosilion.afterimage.repository.join.standard;

import com.prosilion.afterimage.entity.join.standard.EventEntityIdentifierTagEntity;
import com.prosilion.afterimage.repository.join.EventEntityAbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventEntityIdentifierTagEntityRepository<T extends EventEntityIdentifierTagEntity> extends EventEntityAbstractTagEntityRepository<T> {
  default String getCode() {
    return "d";
  }
}
