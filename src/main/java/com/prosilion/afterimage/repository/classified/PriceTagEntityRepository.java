package com.prosilion.afterimage.repository.classified;

import com.prosilion.afterimage.entity.classified.PriceTagEntity;
import com.prosilion.afterimage.repository.AbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceTagEntityRepository<T extends PriceTagEntity> extends AbstractTagEntityRepository<T> {
}
