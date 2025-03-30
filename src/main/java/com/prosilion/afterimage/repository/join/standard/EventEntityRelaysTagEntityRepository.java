package com.prosilion.afterimage.repository.join.standard;

import com.prosilion.afterimage.entity.join.standard.EventEntityRelaysTagEntity;
import com.prosilion.afterimage.repository.join.EventEntityAbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventEntityRelaysTagEntityRepository<T extends EventEntityRelaysTagEntity> extends EventEntityAbstractTagEntityRepository<T> {
  default String getCode() {
    return "relays";
  }
}
