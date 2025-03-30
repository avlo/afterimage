package com.prosilion.afterimage.service.message.close;

import com.prosilion.afterimage.service.message.MessageService;
import lombok.NonNull;
import nostr.event.message.CloseMessage;

public interface CloseMessageServiceIF<T extends CloseMessage> extends MessageService<T> {
  void processIncoming(T reqMessage, String sessionId);
  void removeSubscriberBySessionId(String sessionId);
  void removeSubscriberBySubscriberId(@NonNull String subscriberId);
  String getCommand();
  void closeSession(@NonNull String sessionId);
}
