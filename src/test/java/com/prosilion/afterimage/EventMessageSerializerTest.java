package com.prosilion.afterimage;

import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.codec.IDecoder;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.GenericEventRecord;
import com.prosilion.nostr.event.internal.Relay;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.EventTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.nostr.user.Signature;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonComparator;
import org.springframework.test.json.JsonComparison;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ActiveProfiles("test")
public class EventMessageSerializerTest {
  private final static String url = "ws://localhost:5555";
  private final Relay relay = new Relay(url);

  private final GenericEventRecord genericEventRecordWithAddressTag;
  private final GenericEventRecord genericEventRecordWithEventTag;

  public EventMessageSerializerTest() {
    this.genericEventRecordWithAddressTag = new GenericEventRecord(
        "5f66a36101d3d152c6270e18f5622d1f8bce4ac5da9ab62d7c3cc0006e590001",
        new PublicKey("bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984"),
        1111111111111L,
        Kind.BADGE_AWARD_EVENT,
        List.of(
            new AddressTag(Kind.BADGE_DEFINITION_EVENT,
                new PublicKey("bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984"),
                new IdentifierTag(ExpressionTest.UNIT_UPVOTE),
                relay)),
        "matching kind, author, identity-tag filter test",
        new Signature("86f25c161fec51b9e441bdb2c09095d5f8b92fdce66cb80d9ef09fad6ce53eaa14c5e16787c42f5404905536e43ebec0e463aee819378a4acbe412c533e60546"));

    this.genericEventRecordWithEventTag = new GenericEventRecord(
        "5f66a36101d3d152c6270e18f5622d1f8bce4ac5da9ab62d7c3cc0006e590001",
        new PublicKey("bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984"),
        1111111111111L,
        Kind.BADGE_AWARD_EVENT,
        List.of(
            new EventTag("bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984", url)),
        "matching kind, author, identity-tag filter test",
        new Signature("86f25c161fec51b9e441bdb2c09095d5f8b92fdce66cb80d9ef09fad6ce53eaa14c5e16787c42f5404905536e43ebec0e463aee819378a4acbe412c533e60546"));

//    BadgeAwardGenericEvent<BadgeDefinitionAwardEvent> badgeAwardGenericEvent = new BadgeAwardGenericEvent<BadgeDefinitionAwardEvent>(
//        Identity.generateRandomIdentity(),
//        new PublicKey("bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984"),
//        1111111111111L,
//        Kind.BADGE_AWARD_EVENT,
//        List.of(
//            new AddressTag(Kind.BADGE_DEFINITION_EVENT,
//                new PublicKey("bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984"),
//                new IdentifierTag(ExpressionTest.UNIT_UPVOTE),
//                relay)),
//        "matching kind, author, identity-tag filter test",
//        new Signature("86f25c161fec51b9e441bdb2c09095d5f8b92fdce66cb80d9ef09fad6ce53eaa14c5e16787c42f5404905536e43ebec0e463aee819378a4acbe412c533e60546"));
  }

  @Test
  void testStringEventMessageAddressTagGenericEventKindEncoder() throws IOException, NostrException {
    getStringEquals(
        new EventMessage(
            genericEventRecordWithAddressTag.asGenericEventRecord()),
        expectedStringWithAddressTagShouldMatch());
  }

  @Test
  void testJsonEventMessageAddressTagGenericEventKindEncoder() throws IOException, NostrException {
    getJsonEquals(
        new EventMessage(
            genericEventRecordWithAddressTag.asGenericEventRecord()),
        expectedStringWithAddressTagShouldMatch());
  }

  @Test
  void testStringEventMessageEventTagGenericEventKindEncoder() throws IOException, NostrException {
    getStringEquals(
        new EventMessage(
            genericEventRecordWithEventTag.asGenericEventRecord()),
        expectedStringWithEventTagShouldMatch());
  }

  @Test
  void testJsonEventMessageEventTagGenericEventKindEncoder() throws IOException, NostrException {
    getJsonEquals(
        new EventMessage(
            genericEventRecordWithEventTag.asGenericEventRecord()),
        expectedStringWithEventTagShouldMatch());
  }

  private void getStringEquals(EventMessage eventMessage, String expected) throws IOException, NostrException {
    String actual = IDecoder.I_DECODER_MAPPER_AFTERBURNER.writeValueAsString(eventMessage);

    assertEquals(expected, actual);
  }

  private void getJsonEquals(EventMessage eventMessage, String expected) throws IOException, NostrException {
    String actual = IDecoder.I_DECODER_MAPPER_AFTERBURNER.writeValueAsString(eventMessage);

    assertEquals(expected, actual);

//    String actual = eventMessage.encode();
    JsonComparator comparator = (expectedJson, actualJson) -> JsonComparison.match();
    log.debug("");
    log.debug("");
    log.debug(actual);
    log.debug("");
    log.debug("-------------");
    log.debug("");
    log.debug(actual);
    log.debug("");
    log.debug("");

    assertEquals(JsonComparison.Result.MATCH, comparator.compare(actual, actual).getResult());
    assertEquals(JsonComparison.Result.MATCH, comparator.compare(expectedStringWithAddressTagShouldMatch(), actual).getResult());
  }

  private String expectedStringWithAddressTagShouldMatch() {
    return """
        ["EVENT",{"id":"5f66a36101d3d152c6270e18f5622d1f8bce4ac5da9ab62d7c3cc0006e590001","pubkey":"bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984","created_at":1111111111111,"kind":8,"tags":[["a","30009:bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984:UNIT_UPVOTE","ws://localhost:5555"]],"content":"matching kind, author, identity-tag filter test","sig":"86f25c161fec51b9e441bdb2c09095d5f8b92fdce66cb80d9ef09fad6ce53eaa14c5e16787c42f5404905536e43ebec0e463aee819378a4acbe412c533e60546"}]""";
  }

  private String expectedStringWithEventTagShouldMatch() {
    return """
        ["EVENT",{"id":"5f66a36101d3d152c6270e18f5622d1f8bce4ac5da9ab62d7c3cc0006e590001","pubkey":"bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984","created_at":1111111111111,"kind":8,"tags":[["e","bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984","ws://localhost:5555"]],"content":"matching kind, author, identity-tag filter test","sig":"86f25c161fec51b9e441bdb2c09095d5f8b92fdce66cb80d9ef09fad6ce53eaa14c5e16787c42f5404905536e43ebec0e463aee819378a4acbe412c533e60546"}]""";
  }
}
