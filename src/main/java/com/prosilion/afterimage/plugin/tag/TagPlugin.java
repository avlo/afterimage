package com.prosilion.afterimage.plugin.tag;

import com.prosilion.afterimage.dto.AbstractTagDto;
import com.prosilion.afterimage.entity.AbstractTagEntity;
import com.prosilion.afterimage.entity.join.EventEntityAbstractEntity;
import com.prosilion.afterimage.repository.AbstractTagEntityRepository;
import com.prosilion.afterimage.repository.join.EventEntityAbstractTagEntityRepository;
import nostr.event.BaseTag;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagPlugin<
    P extends BaseTag,
    Q extends AbstractTagEntityRepository<R>, // dto table
    R extends AbstractTagEntity, // dto to return
    S extends EventEntityAbstractEntity, // @MappedSuperclass for below
    T extends EventEntityAbstractTagEntityRepository<S>> // event -> dto join table
{
  String getCode();
  AbstractTagDto getTagDto(P baseTag);
  AbstractTagEntityRepository<R> getRepo();
  EventEntityAbstractTagEntityRepository<S> getJoin();
  S getEventEntityTagEntity(Long eventId, Long baseTagId);

  default R convertDtoToEntity(P baseTag) {
    return (R) getTagDto(baseTag).convertDtoToEntity();
  }

  default List<R> getTags(Long eventId) {
    return getJoin()
        .findByEventId(eventId)
        .stream()
        .map(event -> Optional.of(
                getRepo().findById(
                    event.getId()))
            .orElseGet(Optional::empty).stream().toList())
        .flatMap(Collection::stream).distinct().toList();
  }

  default void saveTag(Long eventId, P baseTag) {
    getJoin().save(
        getEventEntityTagEntity(
            eventId,
            getRepo().save(
                convertDtoToEntity(baseTag)).getId()));
  }
}
