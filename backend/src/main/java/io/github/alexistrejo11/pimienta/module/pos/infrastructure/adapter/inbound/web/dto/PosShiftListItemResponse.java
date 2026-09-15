package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.pos.core.application.PosShiftListItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "PosShiftListItemResponse")
public record PosShiftListItemResponse(
    UUID shiftId,
    long headquarterId,
    UUID deviceId,
    Long cashierOperatorId,
    String cashierDisplayName,
    String deviceName,
    String deviceVisibleCode,
    long openingCashCentavos,
    Instant openedAt,
    Instant closedAt,
    String status,
    Long expectedCashCentavos,
    Long countedCashCentavos,
    Long differenceCentavos) {

  public static PosShiftListItemResponse from(PosShiftListItem item) {
    var shift = item.shift();
    return new PosShiftListItemResponse(
        shift.id(),
        shift.headquarterId(),
        shift.deviceId(),
        shift.cashierOperatorId(),
        item.cashierDisplayName(),
        item.deviceName(),
        item.deviceVisibleCode(),
        shift.openingCashCentavos(),
        shift.openedAt(),
        shift.closedAt(),
        shift.status(),
        shift.expectedCashCentavos(),
        shift.countedCashCentavos(),
        shift.differenceCentavos());
  }
}
