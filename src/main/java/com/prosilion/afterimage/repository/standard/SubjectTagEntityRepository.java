package com.prosilion.afterimage.repository.standard;

import com.prosilion.afterimage.entity.standard.SubjectTagEntity;
import com.prosilion.afterimage.repository.AbstractTagEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectTagEntityRepository<T extends SubjectTagEntity> extends AbstractTagEntityRepository<T> {
}
