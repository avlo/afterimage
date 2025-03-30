package com.prosilion.afterimage.repository.join.standard;

import com.prosilion.afterimage.entity.join.standard.EventEntitySubjectTagEntity;
import com.prosilion.afterimage.repository.join.EventEntityAbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventEntitySubjectTagEntityRepository<T extends EventEntitySubjectTagEntity> extends EventEntityAbstractTagEntityRepository<T> {
  default String getCode() {
    return "subject";
  }
}
