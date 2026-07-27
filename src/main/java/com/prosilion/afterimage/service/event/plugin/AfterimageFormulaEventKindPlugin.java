package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.CuratedBadgeDefinitionGenericEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.PubKeyTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.superconductor.autoconfigure.base.service.event.curated.CacheCuratedBadgeDefinitionGenericEventService;
import com.prosilion.superconductor.autoconfigure.base.service.event.curated.CacheCuratedFormulaEventService;
import com.prosilion.superconductor.base.service.event.plugin.EventPlugin;
import com.prosilion.superconductor.base.service.event.plugin.kind.NonPublishingEventKindPlugin;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

@Slf4j
public class AfterimageFormulaEventKindPlugin extends NonPublishingEventKindPlugin {
  private final CacheCuratedFormulaEventService cacheCuratedFormulaEventService;
  private final CacheCuratedBadgeDefinitionGenericEventService cacheCuratedBadgeDefinitionGenericEventService;

  public AfterimageFormulaEventKindPlugin(
     @NonNull CacheCuratedFormulaEventService cacheCuratedFormulaEventService,
     @NonNull CacheCuratedBadgeDefinitionGenericEventService cacheCuratedBadgeDefinitionGenericEventService,
     @NonNull EventPlugin eventPlugin) {
    super(eventPlugin);
    this.cacheCuratedFormulaEventService = cacheCuratedFormulaEventService;
    this.cacheCuratedBadgeDefinitionGenericEventService = cacheCuratedBadgeDefinitionGenericEventService;
  }

  @Override
  public Optional<GenericEventRecord> processIncomingEvent(@NonNull EventIF event, @NonNull Relay fromRelay) {
    if (cacheCuratedFormulaEventService.getEvent(
       event.getId(),
       event.getRelayTag().map(RelayTag::getRelay).orElseThrow()).isPresent())
      return Optional.of(event.asGenericEventRecord());

    if (cacheCuratedFormulaEventService.getBy(
       new PubKeyTag(event.getPublicKey()),
       event.requireFirstTag(IdentifierTag.class),
       fromRelay).isPresent())
      return Optional.of(event.asGenericEventRecord());

    CuratedBadgeDefinitionGenericEvent curatedBadgeDefinitionEvent =
       cacheCuratedBadgeDefinitionGenericEventService.getByDirect(event.requireFirstTag(AddressTag.class)).orElseThrow();

    return Optional.of(curatedBadgeDefinitionEvent.asGenericEventRecord());
  }

  @Override
  public Kind getKind() {
    return Kind.ARBITRARY_CUSTOM_APP_DATA;
  }
}
