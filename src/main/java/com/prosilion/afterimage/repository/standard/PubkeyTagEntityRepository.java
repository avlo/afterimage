package com.prosilion.afterimage.repository.standard;

import com.prosilion.afterimage.entity.standard.PubkeyTagEntity;
import com.prosilion.afterimage.repository.AbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PubkeyTagEntityRepository<T extends PubkeyTagEntity> extends AbstractTagEntityRepository<T> {
}
