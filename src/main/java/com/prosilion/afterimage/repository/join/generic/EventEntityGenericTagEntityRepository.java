package com.prosilion.afterimage.repository.join.generic;

import com.prosilion.afterimage.entity.join.generic.EventEntityGenericTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventEntityGenericTagEntityRepository extends JpaRepository<EventEntityGenericTagEntity, Long> {
  List<EventEntityGenericTagEntity> findByEventId(Long eventId);
}
