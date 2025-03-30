package com.prosilion.afterimage.repository.join;

import com.prosilion.afterimage.entity.join.EventEntityAbstractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface EventEntityAbstractTagEntityRepository<T extends EventEntityAbstractEntity> extends JpaRepository<T, Long> {
  List<T> findByEventId(Long eventId);
  String getCode();
}
