package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request body for {@code PUT balances/{id}/identity}: the {@code identity_id}
 * to attach.
 */
public final class UpdateBalanceIdentity {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private UpdateBalanceIdentity() {}

  public static UpdateBalanceIdentity create() {
    return new UpdateBalanceIdentity();
  }

  public UpdateBalanceIdentity identityId(String identityId) {
    fields.put("identity_id", identityId);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /** Wire body. */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
