package com.prosilion.afterimage.relay;

import com.prosilion.afterimage.util.InvalidReputationReqJsonException;
import com.prosilion.superconductor.service.message.req.ReqMessageServiceIF;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nostr.base.PublicKey;
import nostr.event.Kind;
import nostr.event.filter.AddressTagFilter;
import nostr.event.filter.Filterable;
import nostr.event.filter.Filters;
import nostr.event.message.ReqMessage;
import nostr.event.tag.AddressTag;
import nostr.event.tag.IdentifierTag;
import nostr.id.Identity;

@Slf4j
public class ReputationReqMessageService<T extends ReqMessage> implements ReqMessageServiceIF<T> {
  public static final String ADDRESS_TAG_FILTER = AddressTagFilter.FILTER_KEY;
  private final Identity aImgIdentity;

  private final ReqMessageServiceIF<T> reqMessageService;

  public ReputationReqMessageService(
      @NonNull ReqMessageServiceIF<T> reqMessageService,
      @NonNull Identity aImgIdentity) {
    log.debug("loaded ReputationReqMessageService bean");
    this.reqMessageService = reqMessageService;
    this.aImgIdentity = aImgIdentity;
  }

  @Override
  public void processIncoming(@NonNull T reqMessage, @NonNull String sessionId) {
    try {
      reqMessageService.processIncoming(
          (T) new ReqMessage(sessionId,
              culledByAddressTag(reqMessage)),
          sessionId);
    } catch (InvalidReputationReqJsonException e) {
      processNoticeClientResponse(reqMessage, sessionId, e.getMessage());
    }
  }

  @Override
  public void processNoticeClientResponse(@NonNull T reqMessage, @NonNull String sessionId, @NonNull String errorMessage) {
    reqMessageService.processNoticeClientResponse(reqMessage, sessionId, errorMessage);
  }

  private Filters culledByAddressTag(T reqMessage) throws InvalidReputationReqJsonException {
    return new Filters(
        validateRequiredFilterByAddressTag(reqMessage).stream().map(publicKey ->
                new AddressTagFilter<>(new AddressTag(
                    Kind.REPUTATION.getValue(),
                    publicKey,
                    new IdentifierTag(
//                       TODO: below identiy needs design/thought
                        aImgIdentity.getPublicKey().toHexString())
                )))
            .map(Filterable.class::cast)
            .toList());
  }

  private List<PublicKey> validateRequiredFilterByAddressTag(T reqMessage) throws InvalidReputationReqJsonException {
    return reqMessage.getFiltersList().stream()
        .flatMap(filters ->
            Optional.of(filters.getFilterByType(ADDRESS_TAG_FILTER).stream())
                .orElseThrow(() -> new InvalidReputationReqJsonException(reqMessage, ADDRESS_TAG_FILTER)))
        .map(AddressTagFilter.class::cast)
        .map(AddressTagFilter::getFilterable)
        .map(AddressTag.class::cast)
        .filter(addressTag ->
            Optional.of(
                    addressTag.getKind().equals(Kind.REPUTATION.getValue()))
//            note: below will throw exception if any filter among all filters does not contain 2113
//              may want to change/update this, revisit later
                .orElseThrow(() -> new InvalidReputationReqJsonException(reqMessage, Kind.REPUTATION.getName())))
        .map(AddressTag::getPublicKey)
        .toList();
  }
}
