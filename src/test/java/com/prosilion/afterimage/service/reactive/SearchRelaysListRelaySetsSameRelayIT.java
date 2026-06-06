package com.prosilion.afterimage.service.reactive;

import com.ezylang.evalex.parser.ParseException;
import com.prosilion.afterimage.config.MultiContainerSameRelayTestConfig;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.subdivisions.client.RequestSubscriber;
import com.prosilion.superconductor.base.service.event.EventServiceIF;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Import(MultiContainerSameRelayTestConfig.class)
public class SearchRelaysListRelaySetsSameRelayIT extends AbstractIT {
  @Autowired
  public SearchRelaysListRelaySetsSameRelayIT(
      @NonNull @Qualifier("eventService") EventServiceIF eventServiceIF,
      @NonNull @Value("${afterimage.relay.url}") String afterimageRelayUrl,
      @NonNull @Value("${superconductor.relay.url}") String superconductorRelayUrl) throws ParseException, InterruptedException, IOException {
    super(eventServiceIF, superconductorRelayUrl, afterimageRelayUrl);

    submitSCEvent(
        createUpvoteEvent(submitter, recipient, superconductorRelay),
        superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey()));
    TimeUnit.MILLISECONDS.sleep(100);

    submitRelayEvent(
        createSearchRelaysListEventMessage(superconductorRelay),
        afterimageRelayUrl);
    TimeUnit.MILLISECONDS.sleep(1500);
  }

  @Test
  void searchRelaysListRelaySetsSameRelay() throws IOException, NostrException, InterruptedException {
    RequestSubscriber<BaseMessage> subscriber_1 = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(recipient, defnCreator, afterimageRelayUrl, subscriber_1);

    List<EventIF> subscriber_1_events =
        validateGeneralAfterimageRequestResults(
            getGenericEvents(subscriber_1.getItems()));
    assertEquals(1, (long) subscriber_1_events.size());
    assertEquals("1", subscriber_1_events.getFirst().getContent());

//    submit 2nd SC upvote event
    submitSCEvent(
        createUpvoteEvent(submitter, recipient, superconductorRelay),
        superconductorRelayUrl, badgeAwardEventFilter.apply(recipient.getPublicKey()));

    TimeUnit.MILLISECONDS.sleep(2000); // give time for upvoteEvent to propagate to aImg

    RequestSubscriber<BaseMessage> subscriber_2 = new RequestSubscriber<>();
    submitAfterImageReqWithSubscriber(recipient, defnCreator, afterimageRelayUrl, subscriber_2);

    List<EventIF> subscriber2EventIFs =
        validateGeneralAfterimageRequestResults(
            getGenericEvents(subscriber_2.getItems()));

    log.debug("11111111111111111111111");
    log.debug("11111111111111111111111");
    log.debug(subscriber2EventIFs.stream().map(EventIF::createPrettyPrintJson).collect(Collectors.joining(",\n")));
    log.debug("22222222222222222222222");
    log.debug("22222222222222222222222");

//    assertTrue(subscriber2EventIFs.stream().map(EventIF::getContent).anyMatch("2"::equals));
    assertEquals(1, (long) subscriber2EventIFs.size());
    assertEquals("2", subscriber2EventIFs.getFirst().getContent());

    List<EventIF> subscriber_1_events_2 =
        validateGeneralAfterimageRequestResults(
            getGenericEvents(subscriber_1.getItems()));

    log.debug("33333333333333333333333");
    log.debug("33333333333333333333333");
    log.debug(subscriber_1_events_2.stream().map(EventIF::createPrettyPrintJson).collect(Collectors.joining(",\n")));
    log.debug("44444444444444444444444");
    log.debug("44444444444444444444444");

    assertEquals(1, (long) subscriber_1_events_2.size());
    assertEquals("2", subscriber_1_events_2.getFirst().getContent());

//    TestSubscriber<BaseMessage> afterImageEventsSubscriber_9 = new TestSubscriber<>();
//    final AfterimageMeshRelayService afterimageRepRequestClient_3 = new AfterimageMeshRelayService(afterimageRelayUri);
//    afterimageRepRequestClient_3.send(
//        createAfterImageReqMessage(Factory.generateRandomHex64String(), upvotedUser.getPublicKey()),
//        afterImageEventsSubscriber_9);
//
//    List<BaseMessage> items_8 = afterImageEventsSubscriber_9.getItems();
//    log.debug("  {}", items_8);
//
//    List<EventIF> returnedReqGenericEvents_4 = getGenericEvents(items_8);
//    assertEquals("3", returnedReqGenericEvents_4.getFirst().getContent());
//
//    afterimageRepRequestClient_3.closeSocket();
  }
}
