package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSale;

@Schema(name = "PosSaleReportResponse")
public record PosSaleReportResponse(
    UUID saleId,
    UUID eventId,
    Long headquarterId,
    UUID deviceId,
    UUID shiftId,
    Long cashierOperatorId,
    String folio,
    long grossCentavos,
    long discountCentavos,
    long totalCentavos,
    String status,
    String syncStatus,
    Instant occurredAt,
    boolean containsOpenProduct,
    List<PosSaleLineReportResponse> lines) {

  public record PosSaleLineReportResponse(
      UUID lineId,
      Long productId,
      String lineType,
      String productName,
      String saleCategory,
      int quantity,
      long unitPriceCentavos,
      long subtotalCentavos) {
    public static PosSaleLineReportResponse from(PosSale.Line line) {
      return new PosSaleLineReportResponse(line.lineId(), line.productId(), line.lineType().name(),
          line.productName(), line.saleCategory(), line.quantity(), line.unitPriceCentavos(), line.subtotalCentavos());
    }
  }
}
