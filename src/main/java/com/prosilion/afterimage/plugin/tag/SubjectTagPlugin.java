package com.prosilion.afterimage.plugin.tag;

import com.prosilion.afterimage.dto.standard.SubjectTagDto;
import com.prosilion.afterimage.entity.join.standard.EventEntitySubjectTagEntity;
import com.prosilion.afterimage.entity.standard.SubjectTagEntity;
import com.prosilion.afterimage.repository.join.standard.EventEntitySubjectTagEntityRepository;
import com.prosilion.afterimage.repository.standard.SubjectTagEntityRepository;
import lombok.NonNull;
import nostr.event.tag.SubjectTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SubjectTagPlugin<
    P extends SubjectTag,
    Q extends SubjectTagEntityRepository<R>,
    R extends SubjectTagEntity,
    S extends EventEntitySubjectTagEntity,
    T extends EventEntitySubjectTagEntityRepository<S>> extends AbstractTagPlugin<P, Q, R, S, T> {

  @Autowired
  public SubjectTagPlugin(@NonNull SubjectTagEntityRepository<R> repo, @NonNull EventEntitySubjectTagEntityRepository<S> join) {
    super(repo, join, "subject");
  }

  @Override
  public SubjectTagDto getTagDto(P subjectTag) {
    return new SubjectTagDto(subjectTag);
  }

  @Override
  public S getEventEntityTagEntity(Long eventId, Long subjectTagId) {
    return (S) new EventEntitySubjectTagEntity(eventId, subjectTagId);
  }
}
