package io.github.alexistrejo11.pimienta.module.pos.core.application.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleLineType;

public record IngestPosEventsCommand(List<IngestPosEventItem> events) {

  public record IngestPosEventItem(
      UUID eventId,
      String eventType,
      int schemaVersion,
      UUID deviceId,
      String siteId,
      long deviceSequence,
      UUID aggregateId,
      UUID shiftId,
      Instant occurredAt,
      String payloadJson,
      SaleConfirmedPayload saleConfirmed) {}

  public record SaleConfirmedPayload(
      UUID saleId,
      String folio,
      Long cashierOperatorId,
      long grossCentavos,
      long discountCentavos,
      long totalCentavos,
      String status,
      List<SaleLinePayload> lines,
      List<SalePaymentPayload> payments) {}

  public record SaleLinePayload(
      UUID lineId,
      PosSaleLineType lineType,
      Long productId,
      String productName,
      String saleCategory,
      int quantity,
      String unit,
      long unitPriceCentavos,
      long subtotalCentavos,
      String stockPolicy,
      boolean soldWithNegativeStock,
      boolean soldWhileUnavailable,
      String rawBarcode,
      Long authorizedByOperatorId,
      Instant authorizedAt) {}

  public record SalePaymentPayload(
      UUID paymentId,
      String method,
      long amountCentavos,
      Long tenderedCentavos,
      Long changeCentavos) {}
}
