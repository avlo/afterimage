package com.prosilion.afterimage.repository.standard;

import com.prosilion.afterimage.entity.standard.GeohashTagEntity;
import com.prosilion.afterimage.repository.AbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeohashTagEntityRepository<T extends GeohashTagEntity> extends AbstractTagEntityRepository<T> {
}
