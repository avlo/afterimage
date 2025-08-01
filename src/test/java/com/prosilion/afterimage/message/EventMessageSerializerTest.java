package com.prosilion.afterimage.message;

import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.codec.IDecoder;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.nostr.user.Signature;
import com.prosilion.superconductor.base.service.event.service.GenericEventKind;
import com.prosilion.superconductor.base.service.event.service.GenericEventKindType;
import com.prosilion.superconductor.base.service.event.type.SuperconductorKindType;
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
  @Test
  void testEventMessageGenericEventKindTypeEncoder() throws IOException, NostrException {
    EventMessage eventMessage =
        new EventMessage(
            new GenericEventKindType(
                new GenericEventKind("5f66a36101d3d152c6270e18f5622d1f8bce4ac5da9ab62d7c3cc0006e590001", new PublicKey("bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984"), 1111111111111L, Kind.BADGE_AWARD_EVENT, List.of(new AddressTag(Kind.BADGE_DEFINITION_EVENT, new PublicKey("bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984"), new IdentifierTag(SuperconductorKindType.UPVOTE.getName()))), "matching kind, author, identity-tag filter test", Signature.fromString("86f25c161fec51b9e441bdb2c09095d5f8b92fdce66cb80d9ef09fad6ce53eaa14c5e16787c42f5404905536e43ebec0e463aee819378a4acbe412c533e60546")), SuperconductorKindType.UPVOTE));

    getEqualToJson(eventMessage);
  }

  private void getEqualToJson(EventMessage eventMessage) throws IOException, NostrException {
    String expected = IDecoder.I_DECODER_MAPPER_AFTERBURNER.writeValueAsString(eventMessage);

    String actual = eventMessage.encode();
    JsonComparator comparator = (expectedJson, actualJson) -> JsonComparison.match();
    log.debug("");
    log.debug("");
    log.debug(expected);
    log.debug("");
    log.debug("-------------");
    log.debug("");
    log.debug(actual);
    log.debug("");
    log.debug("");

    assertEquals(JsonComparison.Result.MATCH, comparator.compare(expected, actual).getResult());
    assertEquals(JsonComparison.Result.MATCH, comparator.compare(expectedStringShouldMatch(), actual).getResult());
  }

  private String expectedStringShouldMatch() {
    return """
        ["EVENT",{"id":"5f66a36101d3d152c6270e18f5622d1f8bce4ac5da9ab62d7c3cc0006e590001","pubkey":"bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984","created_at":1111111111111,"kind":8,"tags":[["a","30009:bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984:upvote"]],"content":"matching kind, author, identity-tag filter test","sig":"86f25c161fec51b9e441bdb2c09095d5f8b92fdce66cb80d9ef09fad6ce53eaa14c5e16787c42f5404905536e43ebec0e463aee819378a4acbe412c533e60546"}]
        """;
  }
}
