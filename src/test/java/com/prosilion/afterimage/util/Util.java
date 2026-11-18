package com.prosilion.afterimage.util;

import com.prosilion.nostr.enums.Kind;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.filter.Filters;
import com.prosilion.nostr.filter.event.KindFilter;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
import java.util.List;

public class Util {

  private static final Filters FILTERS = new Filters(
     new KindFilter(Kind.BADGE_AWARD_EVENT),
     new KindFilter(Kind.FOLLOW_SETS),
     new KindFilter(Kind.CURATION_SETS_BADGE_AWARD_EVENT),
     new KindFilter(Kind.CURATION_SETS_BADGE_DEFINITION_EVENT),
     new KindFilter(Kind.CURATION_SETS_FORMULA_EVENT),
     new KindFilter(Kind.BADGE_SETS_EVENT)
  );

  private static final Filters FILTERS_BADGE_AWARD_EVENT_8 = new Filters(
     new KindFilter(Kind.BADGE_AWARD_EVENT));
  
  private static final Filters FILTERS_CURATION_SETS_FORMULA_EVENT_30006 = new Filters(
     new KindFilter(Kind.CURATION_SETS_FORMULA_EVENT));

  private static final Filters FILTERS_FOLLOW_SETS_30000 = new Filters(
     new KindFilter(Kind.FOLLOW_SETS));

  private static final Filters FILTERS_CURATION_SETS_BADGE_AWARD_EVENT_30004 = new Filters(
     new KindFilter(Kind.CURATION_SETS_BADGE_AWARD_EVENT));

  private static final Filters FILTERS_CURATION_SETS_BADGE_DEFINITION_EVENT_30005 = new Filters(
     new KindFilter(Kind.CURATION_SETS_BADGE_DEFINITION_EVENT));

  private static final Filters FILTERS_CURATION_SETS_BADGE_SETS_EVENT_30008 = new Filters(
     new KindFilter(Kind.BADGE_SETS_EVENT));

  static public List<EventIF> queryRemoteAimgUrl(String url, Filters filters) {
    List<EventIF> doit = doit(
       new ReqMessage(
          com.prosilion.nostr.util.Util.generateRandomHex64String(),
          filters),
       url);
    return doit;
  }

  static public List<EventIF> queryRemoteAimgUrl(String url) {
    List<EventIF> doit = doit(
       new ReqMessage(
          com.prosilion.nostr.util.Util.generateRandomHex64String(),
          FILTERS),
       url);
    return doit;
  }

  static public List<EventIF> doit(ReqMessage reqMessage, String url) {
    return getEventIFs(
       new NostrSingleRequestService().send(
          reqMessage,
          url));
  }


  static protected List<EventIF> getEventIFs(List<BaseMessage> messages) {
    return messages.stream()
       .filter(EventMessage.class::isInstance)
       .map(EventMessage.class::cast)
       .map(EventMessage::getEvent)
       .toList();
  }
}
