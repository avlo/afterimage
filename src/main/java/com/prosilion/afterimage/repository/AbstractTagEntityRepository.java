package com.prosilion.afterimage.repository;

import com.prosilion.afterimage.entity.AbstractTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface AbstractTagEntityRepository<T extends AbstractTagEntity> extends JpaRepository<T, Long> {
}
