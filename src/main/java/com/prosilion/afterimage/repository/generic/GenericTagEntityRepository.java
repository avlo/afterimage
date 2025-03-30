package com.prosilion.afterimage.repository.generic;

import com.prosilion.afterimage.entity.generic.GenericTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GenericTagEntityRepository extends JpaRepository<GenericTagEntity, Long> {
}
