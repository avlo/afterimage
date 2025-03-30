package com.prosilion.afterimage.plugin.tag;

import com.prosilion.afterimage.dto.standard.HashtagTagDto;
import com.prosilion.afterimage.entity.join.standard.EventEntityHashtagTagEntity;
import com.prosilion.afterimage.entity.standard.HashtagTagEntity;
import com.prosilion.afterimage.repository.join.standard.EventEntityHashtagTagEntityRepository;
import com.prosilion.afterimage.repository.standard.HashtagTagEntityRepository;
import jakarta.annotation.Nonnull;
import lombok.NonNull;
import nostr.event.tag.HashtagTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HashtagTagPlugin<
    P extends HashtagTag,
    Q extends HashtagTagEntityRepository<R>,
    R extends HashtagTagEntity,
    S extends EventEntityHashtagTagEntity,
    T extends EventEntityHashtagTagEntityRepository<S>> extends AbstractTagPlugin<P, Q, R, S, T> {

  @Autowired
  public HashtagTagPlugin(@Nonnull HashtagTagEntityRepository<R> repo, @NonNull EventEntityHashtagTagEntityRepository<S> join) {
    super(repo, join, "t");
  }

  @Override
  public HashtagTagDto getTagDto(P hashtagTag) {
    return new HashtagTagDto(hashtagTag);
  }

  @Override
  public S getEventEntityTagEntity(Long eventId, Long subjectTagId) {
    return (S) new EventEntityHashtagTagEntity(eventId, subjectTagId);
  }
}
