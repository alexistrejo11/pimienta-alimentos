package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.pos.core.application.PosReportSummaryRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(name = "PosReportSummaryResponse")
public record PosReportSummaryResponse(List<PosReportSummaryItemResponse> rows) {

  public static PosReportSummaryResponse from(List<PosReportSummaryRow> rows) {
    return new PosReportSummaryResponse(rows.stream().map(PosReportSummaryItemResponse::from).toList());
  }

  @Schema(name = "PosReportSummaryItemResponse")
  public record PosReportSummaryItemResponse(
      long headquarterId,
      long salesCentavos,
      long ticketCount,
      long wasteCount,
      long cancellationCount,
      Instant lastShiftClosedAt,
      long openIncidentCount,
       long deviceCount,
       long openProductCentavos,
       long openProductLineCount,
       long openProductTicketCount,
       long openProductPendingReviewCount) {

    static PosReportSummaryItemResponse from(PosReportSummaryRow row) {
      return new PosReportSummaryItemResponse(
          row.headquarterId(),
          row.salesCentavos(),
          row.ticketCount(),
          row.wasteCount(),
          row.cancellationCount(),
          row.lastShiftClosedAt(),
          row.openIncidentCount(),
           row.deviceCount(), row.openProductCentavos(), row.openProductLineCount(),
           row.openProductTicketCount(), row.openProductPendingReviewCount());
    }
  }
}
