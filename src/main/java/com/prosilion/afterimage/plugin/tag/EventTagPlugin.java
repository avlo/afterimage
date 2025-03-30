package com.prosilion.afterimage.plugin.tag;

import com.prosilion.afterimage.dto.standard.EventTagDto;
import com.prosilion.afterimage.entity.join.standard.EventEntityEventTagEntity;
import com.prosilion.afterimage.entity.standard.EventTagEntity;
import com.prosilion.afterimage.repository.join.standard.EventEntityEventTagEntityRepository;
import com.prosilion.afterimage.repository.standard.EventTagEntityRepository;
import lombok.NonNull;
import nostr.event.tag.EventTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventTagPlugin<
    P extends EventTag,
    Q extends EventTagEntityRepository<R>,
    R extends EventTagEntity,
    S extends EventEntityEventTagEntity,
    T extends EventEntityEventTagEntityRepository<S>> extends AbstractTagPlugin<P, Q, R, S, T> {

  @Autowired
  public EventTagPlugin(@NonNull EventTagEntityRepository<R> repo, @NonNull EventEntityEventTagEntityRepository<S> join) {
    super(repo, join, "e");
  }

  @Override
  public EventTagDto getTagDto(P eventTag) {
    return new EventTagDto(eventTag);
  }

  @Override
  public S getEventEntityTagEntity(Long eventId, Long eventTagId) {
    return (S) new EventEntityEventTagEntity(eventId, eventTagId);
  }
}
