package com.prosilion.afterimage.repository;

import com.prosilion.superconductor.entity.standard.PubkeyTagEntity;
import com.prosilion.superconductor.repository.standard.PubkeyTagEntityRepository;
import java.util.List;
import nostr.base.PublicKey;
import org.springframework.stereotype.Repository;

@Repository
public interface ReputationPubkeyTagEntityRepository<T extends PubkeyTagEntity> extends PubkeyTagEntityRepository<T> {
  List<PubkeyTagEntity> getPubkeyTagEntities(PublicKey publicKey);
}
