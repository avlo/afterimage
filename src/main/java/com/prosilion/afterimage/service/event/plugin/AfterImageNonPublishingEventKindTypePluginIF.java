//package com.prosilion.afterimage.service.event.plugin;
//
//import com.prosilion.nostr.user.Identity;
//import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventKindPlugin;
//import com.prosilion.superconductor.service.event.type.AbstractNonPublishingEventKindTypePlugin;
//import com.prosilion.superconductor.service.event.type.EventEntityService;
//import lombok.Getter;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.lang.NonNull;
//
//@Slf4j
//@Getter
//public abstract class AfterImageNonPublishingEventKindTypePluginIF extends AbstractNonPublishingEventKindTypePlugin {
//
//  private final EventEntityService eventEntityService;
//  private final Identity aImgIdentity;
//
//  public AfterImageNonPublishingEventKindTypePluginIF(
//      @NonNull AbstractNonPublishingEventKindPlugin abstractNonPublishingEventKindPlugin,
//      @NonNull Identity aImgIdentity,
//      @NonNull EventEntityService eventEntityService) {
//    super(abstractNonPublishingEventKindPlugin);
//    this.aImgIdentity = aImgIdentity;
//    this.eventEntityService = eventEntityService;
//  }
//}
