package io.github.alexistrejo11.pimienta.module.pos.core.domain;

import java.time.LocalDateTime;

public record PosSyncTombstone(
    Long id, long headquarterId, String entity, String entityId, LocalDateTime createdAt) {

  public static final String ENTITY_PRODUCT = "product";
  public static final String ENTITY_OPERATOR = "operator";

  public static PosSyncTombstone operator(long headquarterId, long operatorId) {
    return new PosSyncTombstone(
        null, headquarterId, ENTITY_OPERATOR, String.valueOf(operatorId), LocalDateTime.now());
  }

  public static PosSyncTombstone product(long headquarterId, long itemId) {
    return new PosSyncTombstone(
        null, headquarterId, ENTITY_PRODUCT, String.valueOf(itemId), LocalDateTime.now());
  }
}
