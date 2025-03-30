package com.prosilion.afterimage.repository.standard;

import com.prosilion.afterimage.entity.standard.EventTagEntity;
import com.prosilion.afterimage.repository.AbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventTagEntityRepository<T extends EventTagEntity> extends AbstractTagEntityRepository<T> {
}
