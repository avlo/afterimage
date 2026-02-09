package com.prosilion.afterimage.service.event.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.prosilion.afterimage.InvalidKindException;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.BaseEvent;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.filter.Filterable;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.tag.RelayTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.base.cache.CacheServiceIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.plugin.kind.NonPublishingEventKindPlugin;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;

@Slf4j
public abstract class AbstractRelayAnnouncementEventPlugin extends NonPublishingEventKindPlugin {
  private final CacheServiceIF cacheServiceIF;
  private final Identity aImgIdentity;

  public AbstractRelayAnnouncementEventPlugin(
      @NonNull EventKindPluginIF eventKindPlugin,
      @NonNull @Qualifier("redisCacheService") CacheServiceIF cacheServiceIF,
      @NonNull Identity aImgIdentity) {
    super(eventKindPlugin);
    this.cacheServiceIF = cacheServiceIF;
    this.aImgIdentity = aImgIdentity;
  }

  @SneakyThrows
  @Override
  public <T extends BaseEvent> void processIncomingEvent(@NonNull T relaysEvent) {
    log.debug("processing incoming event: [{}]", relaysEvent);

    InvalidKindException.testBoolean(
        relaysEvent.getKind().equals(getKind()),
        relaysEvent.getKind().getName(), List.of(getKind().getName()));

    Set<String> eventRelays = getRelayTag(relaysEvent)
        .collect(Collectors.toSet());

    Set<Stream<String>> existingKnownRelays = cacheServiceIF.getByKind(getKind()).stream()
        .map(AbstractRelayAnnouncementEventPlugin::getRelayTag)
        .collect(Collectors.toSet());

    Set<String> uniqueNewRelays = Sets.difference(
        eventRelays,
        existingKnownRelays);

    if (uniqueNewRelays.isEmpty()) {
      log.debug("did not discover any new unique relays, so just return");
      return;
    }

    log.debug("uniqueNewRelays: [{}]", uniqueNewRelays);
//    TODO::UNIQUE-RELAY-IDENTIFIER-TAG
    super.processIncomingEvent(createEvent(aImgIdentity, new IdentifierTag("TODO::UNIQUE-RELAY-IDENTIFIER-TAG"), uniqueNewRelays.stream()));

    processIncomingEventAuth(uniqueNewRelays);
  }

  protected abstract void processIncomingEventAuth(@NonNull Set<String> uniqueNewRelays) throws JsonProcessingException;

  abstract protected BaseEvent createEvent(@NonNull Identity identity, @NonNull IdentifierTag identifierTag, @NonNull Stream<String> uniqueNewAImgRelays);

  abstract protected Filters getFilters();

  public abstract Kind getKind();

  private static Stream<String> getRelayTag(EventIF eventIF) {
    return Filterable.getTypeSpecificTagsStream(RelayTag.class, eventIF)
        .map(RelayTag::getRelay)
        .map(Relay::getUrl);
  }

  protected static String generateRandomHex64String() {
    return UUID.randomUUID().toString().concat(UUID.randomUUID().toString()).replaceAll("[^A-Za-z0-9]", "");
  }
}
