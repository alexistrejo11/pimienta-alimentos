package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosShift;
import java.time.Instant; import java.util.UUID;

public record PosShiftResponse(UUID shiftId,long headquarterId,UUID deviceId,Long cashierOperatorId,long openingCashCentavos,Instant openedAt,Instant closedAt,String status,Long expectedCashCentavos,Long countedCashCentavos,Long differenceCentavos) {
  public static PosShiftResponse from(PosShift x){return new PosShiftResponse(x.id(),x.headquarterId(),x.deviceId(),x.cashierOperatorId(),x.openingCashCentavos(),x.openedAt(),x.closedAt(),x.status(),x.expectedCashCentavos(),x.countedCashCentavos(),x.differenceCentavos());}
}
