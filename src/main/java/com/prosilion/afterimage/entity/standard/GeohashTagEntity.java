package com.prosilion.afterimage.entity.standard;

import com.prosilion.afterimage.dto.AbstractTagDto;
import com.prosilion.afterimage.dto.standard.GeohashTagDto;
import com.prosilion.afterimage.entity.AbstractTagEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import nostr.event.BaseTag;
import nostr.event.tag.GeohashTag;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "geohash_tag")
public class GeohashTagEntity extends AbstractTagEntity {
  private String location;
  private List<String> filterField;

  public GeohashTagEntity(@NonNull GeohashTag geohashTag) {
    super("g");
    this.location = geohashTag.getLocation();
    this.filterField = List.of(this.location);
  }

  @Override
  @Transient
  public BaseTag getAsBaseTag() {
    return new GeohashTag(location);
  }

  @Override
  public AbstractTagDto convertEntityToDto() {
    return new GeohashTagDto(new GeohashTag(location));
  }
}
