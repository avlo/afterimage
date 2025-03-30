package com.prosilion.afterimage.entity.join.generic;

import com.prosilion.afterimage.entity.join.EventEntityAbstractEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "event-generic_tag-join")
public class EventEntityGenericTagEntity extends EventEntityAbstractEntity {
  private Long genericTagId;

  public EventEntityGenericTagEntity(Long eventId, Long genericTagId) {
    super.setEventId(eventId);
    this.genericTagId = genericTagId;
  }
}
