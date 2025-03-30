package com.prosilion.afterimage.plugin.tag;

import com.prosilion.afterimage.dto.standard.RelaysTagDto;
import com.prosilion.afterimage.entity.join.standard.EventEntityRelaysTagEntity;
import com.prosilion.afterimage.entity.standard.RelaysTagEntity;
import com.prosilion.afterimage.repository.join.standard.EventEntityRelaysTagEntityRepository;
import com.prosilion.afterimage.repository.standard.RelaysTagEntityRepository;
import jakarta.annotation.Nonnull;
import lombok.NonNull;
import nostr.event.tag.RelaysTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RelaysTagPlugin<
    P extends RelaysTag,
    Q extends RelaysTagEntityRepository<R>,
    R extends RelaysTagEntity,
    S extends EventEntityRelaysTagEntity,
    T extends EventEntityRelaysTagEntityRepository<S>> extends AbstractTagPlugin<P, Q, R, S, T> {

  @Autowired
  public RelaysTagPlugin(@Nonnull RelaysTagEntityRepository<R> repo, @NonNull EventEntityRelaysTagEntityRepository<S> join) {
    super(repo, join, "relays");
  }

  @Override
  public RelaysTagDto getTagDto(P relaysTag) {
    return new RelaysTagDto(relaysTag);
  }

  @Override
  public S getEventEntityTagEntity(Long eventId, Long relaysTagId) {
    return (S) new EventEntityRelaysTagEntity(eventId, relaysTagId);
  }
}
