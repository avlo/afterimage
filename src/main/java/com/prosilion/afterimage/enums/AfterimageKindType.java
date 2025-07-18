package com.prosilion.afterimage.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AfterimageKindType implements KindTypeIF {
  REPUTATION(Kind.BADGE_AWARD_EVENT, Kind.BADGE_DEFINITION_EVENT, "REPUTATION");

  private final Kind kind;
  private final Kind kindDefinition;

  @JsonValue
  private final String name;


  @Override
  public KindTypeIF[] getValues() {
    return AfterimageKindType.values();
  }

  @Generated
  public Kind getKind() {
    return this.kind;
  }

  @Generated
  public Kind getKindDefinition() {
    return this.kindDefinition;
  }

  @Generated
  public String getName() {
    return this.name;
  }
}
