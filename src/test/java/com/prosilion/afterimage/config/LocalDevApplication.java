package com.prosilion.afterimage.config;

import com.prosilion.afterimage.AfterimageApplication;
import com.prosilion.afterimage.calculator.UnitReputationCalculator;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.FollowSetsEvent;
import com.prosilion.nostr.event.FollowSetsEvent.EventTagAddressTagPair;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.Identity;
import com.prosilion.superconductor.autoconfigure.redis.config.DataLoaderRedisIF;
import com.prosilion.superconductor.base.service.event.service.plugin.EventKindPluginIF;
import com.prosilion.superconductor.base.service.event.type.EventPluginIF;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class LocalDevApplication {

  public static final String EVENT_ID_666 = "6666666666666666666666666666666666666666666666666666666666666666";
  public static final String EVENT_ID_777 = "7777777777777777777777777777777777777777777777777777777777777777";

  public static void main(String[] args) {
    SpringApplication.from(AfterimageApplication::main).with(LocalDevTestcontainersConfig.class).run(args);
  }

  @Bean
  public FollowSetsDataLoaderRedis followSetsDataLoaderRedis(
      @NonNull EventKindPluginIF afterimageFollowSetsEventPlugin,
      @NonNull String afterimageRelayUrl) {
    Identity authorIdentity = Identity.generateRandomIdentity();
    FollowSetsEvent followSetsEvent = new FollowSetsEvent(
        authorIdentity,
        Identity.generateRandomIdentity().getPublicKey(),
        new IdentifierTag(
            UnitReputationCalculator.class.getCanonicalName()),
        List.of(
            createPair(authorIdentity, EVENT_ID_666, afterimageRelayUrl),
            createPair(authorIdentity, EVENT_ID_777, afterimageRelayUrl)),
        UnitReputationCalculator.class.getName());
    System.out.println("aasdfasdfadsfasd");
    return new FollowSetsDataLoaderRedis(afterimageFollowSetsEventPlugin, followSetsEvent);
  }

  private static EventTagAddressTagPair createPair(Identity authorIdentity, String eventId, String afterimageRelayUrl) {
    return new EventTagAddressTagPair(
        new EventTag(
            eventId,
            afterimageRelayUrl),
        new AddressTag(
            Kind.BADGE_AWARD_EVENT,
            authorIdentity.getPublicKey(),
            new IdentifierTag(
                SuperconductorKindType.UNIT_UPVOTE
//                AfterimageKindType.REPUTATION
                    .getName())));
  }

  public static class FollowSetsDataLoaderRedis implements DataLoaderRedisIF {
    private final EventPluginIF eventPlugin;
    private final FollowSetsEvent followSetsEvent;

    public FollowSetsDataLoaderRedis(
        @NonNull @Qualifier("eventPlugin") EventPluginIF eventPlugin,
        @NonNull FollowSetsEvent followSetsEvent) {
      this.eventPlugin = eventPlugin;
      this.followSetsEvent = followSetsEvent;
    }

    @Override
    public void run(String... args) {
      eventPlugin.processIncomingEvent(followSetsEvent);
    }
  }
}
