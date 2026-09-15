package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosShiftAdminUseCases.ShiftReconciliationDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(name = "PosShiftReconciliationResponse")
public record PosShiftReconciliationResponse(
    PosShiftListItemResponse shift,
    long openingCashCentavos,
    long cashSalesCentavos,
    long cardSalesCentavos,
    long courtesySalesCentavos,
    long withdrawalsCentavos,
    long depositsCentavos,
    Long expectedCashCentavos,
    Long countedCashCentavos,
    Long differenceCentavos,
    long saleTicketCount,
    List<CashMovement> cashMovements,
    List<CashCount> cashCounts) {

  public static PosShiftReconciliationResponse from(ShiftReconciliationDetail detail) {
    var sales = detail.salesSummary();
    long withdrawals = 0;
    long deposits = 0;
    var movements =
        detail.movements().stream()
            .map(
                m ->
                    new CashMovement(
                        m.id(),
                        m.type(),
                        m.amountCentavos(),
                        m.folio(),
                        m.reason(),
                        m.occurredAt()))
            .toList();
    for (var m : detail.movements()) {
      if ("WITHDRAWAL".equals(m.type())) {
        withdrawals += m.amountCentavos();
      } else if ("DEPOSIT".equals(m.type())) {
        deposits += m.amountCentavos();
      }
    }
    var shiftRow = detail.listItem();
    var shift = shiftRow.shift();
    return new PosShiftReconciliationResponse(
        PosShiftListItemResponse.from(shiftRow),
        shift.openingCashCentavos(),
        sales.cashSalesCentavos(),
        sales.cardSalesCentavos(),
        sales.courtesySalesCentavos(),
        withdrawals,
        deposits,
        shift.expectedCashCentavos(),
        shift.countedCashCentavos(),
        shift.differenceCentavos(),
        sales.saleTicketCount(),
        movements,
        detail.counts().stream()
            .map(c -> new CashCount(c.id(), c.totalCentavos(), c.denominations(), c.submittedAt()))
            .toList());
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
