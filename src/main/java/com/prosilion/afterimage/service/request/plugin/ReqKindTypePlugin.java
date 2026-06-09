package com.prosilion.afterimage.service.request.plugin;

import com.prosilion.afterimage.InvalidReputationReqJsonException;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.user.Identity;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ReqKindTypePlugin implements ReqKindTypePluginIF {
  @Getter
  private final Identity aImgIdentity;

  public ReqKindTypePlugin(@NonNull Identity aImgIdentity) {
    this.aImgIdentity = aImgIdentity;
  }

  protected Filterable matchFilterableKey(List<Filters> filtersList, String key) {
    log.debug("matchFilterableKey: [{}]\nagainst List<Filters>:\n{}", key,
       filtersList.stream()
          .map(filters -> filters.toString(2))
          .collect(Collectors.joining(",\n")));

    Filterable filterable = filtersList.stream()
       .map(filters1 ->
          filters1.getFilterByType(key))
       .flatMap(Collection::stream)
       .findFirst().orElseThrow(() -> new InvalidReputationReqJsonException(filtersList, key.concat(" tag")));
    log.debug("matched filterable: [{}]", filterable.getFilterKey());
    return filterable;
  }
}
