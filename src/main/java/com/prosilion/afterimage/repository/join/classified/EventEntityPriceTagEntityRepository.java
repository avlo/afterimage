package com.prosilion.afterimage.repository.join.classified;

import com.prosilion.afterimage.entity.join.classified.EventEntityPriceTagEntity;
import com.prosilion.afterimage.repository.join.EventEntityAbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventEntityPriceTagEntityRepository<T extends EventEntityPriceTagEntity> extends EventEntityAbstractTagEntityRepository<T> {
  default String getCode() {
    return "price";
  }
}
