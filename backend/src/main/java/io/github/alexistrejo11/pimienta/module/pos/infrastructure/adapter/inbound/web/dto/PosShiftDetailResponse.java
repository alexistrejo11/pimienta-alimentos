package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosShiftAdminUseCases.ShiftDetail;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PosShiftDetailResponse(
    PosShiftListItemResponse shift,
    List<CashMovement> cashMovements,
    List<CashCount> cashCounts,
    long cashReconciledCentavos) {

  public static PosShiftDetailResponse from(ShiftDetail x) {
    return new PosShiftDetailResponse(
        PosShiftListItemResponse.from(x.listItem()),
        x.movements()
            .stream()
            .map(
                m ->
                    new CashMovement(
                        m.id(),
                        m.type(),
                        m.amountCentavos(),
                        m.folio(),
                        m.reason(),
                        m.occurredAt()))
            .toList(),
        x.counts()
            .stream()
            .map(c -> new CashCount(c.id(), c.totalCentavos(), c.denominations(), c.submittedAt()))
            .toList(),
        x.listItem().shift().differenceCentavos() == null
            ? 0
            : x.listItem().shift().differenceCentavos());
  }

  public record CashMovement(
      UUID movementId,
      String type,
      long amountCentavos,
      String folio,
      String reason,
      Instant occurredAt) {}

  public record CashCount(
      UUID countId, long totalCentavos, String denominations, Instant submittedAt) {}
}
