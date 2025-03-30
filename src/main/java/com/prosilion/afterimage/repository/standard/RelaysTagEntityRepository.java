package com.prosilion.afterimage.repository.standard;

import com.prosilion.afterimage.entity.standard.RelaysTagEntity;
import com.prosilion.afterimage.repository.AbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RelaysTagEntityRepository<T extends RelaysTagEntity> extends AbstractTagEntityRepository<T> {
}
