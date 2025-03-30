package com.prosilion.afterimage.repository.standard;

import com.prosilion.afterimage.entity.standard.HashtagTagEntity;
import com.prosilion.afterimage.repository.AbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HashtagTagEntityRepository<T extends HashtagTagEntity> extends AbstractTagEntityRepository<T> {
}
