package com.prosilion.afterimage.repository.standard;

import com.prosilion.afterimage.entity.standard.IdentifierTagEntity;
import com.prosilion.afterimage.repository.AbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentifierTagEntityRepository<T extends IdentifierTagEntity> extends AbstractTagEntityRepository<T> {
}
