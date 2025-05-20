package com.prosilion.afterimage.service.event;

import com.prosilion.afterimage.repository.ReputationPubkeyTagEntityRepository;
import com.prosilion.superconductor.entity.AbstractTagEntity;
import com.prosilion.superconductor.entity.EventEntity;
import com.prosilion.superconductor.entity.standard.PubkeyTagEntity;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import java.util.List;
import lombok.NonNull;
import nostr.base.PublicKey;
import nostr.event.impl.GenericEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReputationEventEntityService<T extends GenericEvent> {
  private final EventEntityService<T> eventEntityService;
  private final ReputationPubkeyTagEntityRepository<PubkeyTagEntity> repo;

  @Autowired
  public ReputationEventEntityService(
      @NonNull EventEntityService<T> eventEntityService,
      @NonNull ReputationPubkeyTagEntityRepository<PubkeyTagEntity> repo) {
    this.eventEntityService = eventEntityService;
    this.repo = repo;
  }

  public List<T> getByReferencePublicKey(@NonNull PublicKey publicKey) {
    List<PubkeyTagEntity> pubkeyTagEntities = repo.getPubkeyTagEntities(publicKey);
    List<Long> list = pubkeyTagEntities.stream().map(AbstractTagEntity::getId).toList();
    List<T> list1 = list.stream().map(eventEntityService::getEventById).toList();
    return list1;
  }
}
