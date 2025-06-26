package com.prosilion.afterimage.service.request;

import com.prosilion.nostr.enums.KindTypeIF;
import java.util.List;

public interface ReqKindTypeServiceIF extends ReqKindServiceIF {
  List<KindTypeIF> getKindTypes();
}
