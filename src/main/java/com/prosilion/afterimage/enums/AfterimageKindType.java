package com.prosilion.afterimage.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.tag.ExternalIdentityTag;
import com.prosilion.superconductor.base.service.event.plugin.kind.type.KindTypeIF;
import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AfterimageKindType implements KindTypeIF {
  BADGE_AWARD_REPUTATION_KIND_TYPE(Kind.BADGE_AWARD_EVENT, Kind.BADGE_AWARD_EVENT, "badge_award_reputation"),
  BADGE_DEFINITION_REPUTATION_KIND_TYPE(Kind.BADGE_DEFINITION_EVENT, Kind.BADGE_DEFINITION_EVENT, "badge_definition_reputation");

  public static final String PLATFORM = "afterimage";
  public static final String PROOF = "TBD_see_AfterimageKindType"; // Runtime.exec("git rev-parse HEAD")

  public static final ExternalIdentityTag BADGE_DEFINITION_REPUTATION_EXTERNAL_IDENTITY_TAG =
     new ExternalIdentityTag(PLATFORM, "badge_definition_reputation", PROOF);

  public static final ExternalIdentityTag BADGE_AWARD_REPUTATION_EXTERNAL_IDENTITY_TAG =
     new ExternalIdentityTag(PLATFORM, "badge_award_reputation", PROOF);

  private final Kind kind;
  private final Kind kindDefinition;

  @JsonValue
  private final String name;

  @Override
  public KindTypeIF[] getValues() {
    return values();
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
