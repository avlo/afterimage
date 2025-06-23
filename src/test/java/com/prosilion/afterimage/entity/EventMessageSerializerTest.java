package com.prosilion.afterimage.entity;

import com.prosilion.afterimage.enums.AfterimageKindType;
import com.prosilion.nostr.codec.IDecoder;
import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.enums.NostrException;
import com.prosilion.nostr.event.GenericEventKind;
import com.prosilion.nostr.event.GenericEventKindType;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.tag.AddressTag;
import com.prosilion.nostr.tag.IdentifierTag;
import com.prosilion.nostr.user.PublicKey;
import com.prosilion.nostr.user.Signature;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ActiveProfiles("test")
public class EventMessageSerializerTest {
  @Autowired
  JacksonTester<EventMessage> tester;

  @Test
  void testEventMessageGenericEventKindTypeEncoder() throws IOException, NostrException {
    EventMessage eventMessage = new EventMessage(
        new GenericEventKindType(
            new GenericEventKind(
                "5f66a36101d3d152c6270e18f5622d1f8bce4ac5da9ab62d7c3cc0006e590001",
                new PublicKey("bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984"),
                1111111111111L,
                Kind.valueOf(31923),
                List.of(new AddressTag(
                    Kind.BADGE_AWARD_EVENT,
                    new PublicKey("bbbd79f81439ff794cf5ac5f7bff9121e257f399829e472c7a14d3e86fe76984"),
                    new IdentifierTag(AfterimageKindType.UPVOTE.getName()))),
                "matching kind, author, identity-tag filter test",
                Signature.fromString("86f25c161fec51b9e441bdb2c09095d5f8b92fdce66cb80d9ef09fad6ce53eaa14c5e16787c42f5404905536e43ebec0e463aee819378a4acbe412c533e60546")),
            List.of(
                AfterimageKindType.values())));

    getEqualToJson(eventMessage);
  }

  private void getEqualToJson(EventMessage eventMessage) throws IOException, NostrException {
    String expected = IDecoder.I_DECODER_MAPPER_AFTERBURNER.writeValueAsString(eventMessage);
    assertThat(
        tester.write(eventMessage))
        .isEqualToJson(
            expected)
        .isEqualToJson(
            eventMessage.encode());
  }
}
