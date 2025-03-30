package com.prosilion.afterimage.repository.generic;

import com.prosilion.afterimage.entity.generic.ElementAttributeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ElementAttributeEntityRepository extends JpaRepository<ElementAttributeEntity, Long> {
}
