package com.prosilion.afterimage.plugin.tag;

import com.prosilion.afterimage.dto.classified.PriceTagDto;
import com.prosilion.afterimage.entity.classified.PriceTagEntity;
import com.prosilion.afterimage.entity.join.classified.EventEntityPriceTagEntity;
import com.prosilion.afterimage.repository.classified.PriceTagEntityRepository;
import com.prosilion.afterimage.repository.join.classified.EventEntityPriceTagEntityRepository;
import jakarta.annotation.Nonnull;
import lombok.NonNull;
import nostr.event.tag.PriceTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PriceTagPlugin<
    P extends PriceTag,
    Q extends PriceTagEntityRepository<R>,
    R extends PriceTagEntity,
    S extends EventEntityPriceTagEntity,
    T extends EventEntityPriceTagEntityRepository<S>> extends AbstractTagPlugin<P, Q, R, S, T> {

  @Autowired
  public PriceTagPlugin(@Nonnull PriceTagEntityRepository<R> repo, @NonNull EventEntityPriceTagEntityRepository<S> join) {
    super(repo, join, "price");
  }

  @Override
  public PriceTagDto getTagDto(P priceTag) {
    return new PriceTagDto(priceTag);
  }

  @Override
  public S getEventEntityTagEntity(Long eventId, Long pricetagId) {
    return (S) new EventEntityPriceTagEntity(eventId, pricetagId);
  }
}
