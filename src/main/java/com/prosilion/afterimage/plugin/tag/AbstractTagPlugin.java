package com.prosilion.afterimage.plugin.tag;

import com.prosilion.afterimage.entity.AbstractTagEntity;
import com.prosilion.afterimage.entity.join.EventEntityAbstractEntity;
import com.prosilion.afterimage.repository.AbstractTagEntityRepository;
import com.prosilion.afterimage.repository.join.EventEntityAbstractTagEntityRepository;
import lombok.Getter;
import lombok.NonNull;
import nostr.event.BaseTag;

@Getter
public abstract class AbstractTagPlugin<
    P extends BaseTag,
    Q extends AbstractTagEntityRepository<R>,
    R extends AbstractTagEntity,
    S extends EventEntityAbstractEntity,
    T extends EventEntityAbstractTagEntityRepository<S>> implements TagPlugin<P, Q, R, S, T> {

  private final AbstractTagEntityRepository<R> repo;
  private final EventEntityAbstractTagEntityRepository<S> join;
  private final String code;

  public AbstractTagPlugin(
      @NonNull AbstractTagEntityRepository<R> repo,
      @NonNull EventEntityAbstractTagEntityRepository<S> join,
      @NonNull String code) {
    this.repo = repo;
    this.join = join;
    this.code = code;
  }
}
