package com.prosilion.afterimage.service.event.plugin;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.KindTypeIF;
import com.prosilion.nostr.event.GenericEventKindIF;
import com.prosilion.nostr.event.GenericEventKindTypeIF;
import com.prosilion.superconductor.service.event.service.plugin.EventKindTypePluginIF;
import com.prosilion.superconductor.service.event.type.EventEntityService;
import com.prosilion.superconductor.service.event.type.NonPublishingEventKindTypePlugin;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public abstract class VoteEventKindTypePlugin extends NonPublishingEventKindTypePlugin {
  private final EventEntityService eventEntityService;
  private final EventKindTypePluginIF<KindTypeIF> reputationEventKindTypePlugin;

  public VoteEventKindTypePlugin(
      @NonNull EventEntityService eventEntityService,
      @NonNull EventKindTypePluginIF<KindTypeIF> reputationEventKindTypePlugin) {
    super(reputationEventKindTypePlugin);
    this.eventEntityService = eventEntityService;
    this.reputationEventKindTypePlugin = reputationEventKindTypePlugin;
  }

  @SneakyThrows
  @Override
  public void processIncomingEvent(@NonNull GenericEventKindTypeIF voteEvent) {
    log.debug("VoteEventKindTypePlugin processing incoming VOTE EVENT: [{}]", voteEvent);
//    saves VOTE event without triggering subscriber listener
    eventEntityService.saveEventEntity(voteEvent);
    log.debug("vote saved to db, send vote off to reputationEventKindTypePlugin for rep calculation");
    reputationEventKindTypePlugin.processIncomingEvent(voteEvent);
  }

  @Override
  public void processIncomingEvent(@NonNull GenericEventKindIF voteEvent) {
    log.debug("VoteEventKindTypePlugin processing incoming VOTE EVENT: [{}]", voteEvent);
//    saves VOTE event without triggering subscriber listener
    eventEntityService.saveEventEntity(voteEvent);
    log.debug("vote saved to db, send vote off to reputationEventKindTypePlugin for rep calculation");
    reputationEventKindTypePlugin.processIncomingEvent(voteEvent);
  }
  
  @Override
  public Kind getKind() {
    return Kind.BADGE_AWARD_EVENT;
  }

  abstract public KindTypeIF getKindType();
}
