package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosReportFilterQuery;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosAdminReportUseCases;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosAdminReports;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosReportProducts;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosReportSales;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosReportShiftCloses;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosReportWasteCancellations;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosLedgerEventReportResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosProductReportResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSaleReportResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.mapper.PosWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/admin/reports")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocPosAdminReports
public class PosAdminReportController {

  private final PosAdminReportUseCases reportUseCases;

  public PosAdminReportController(PosAdminReportUseCases reportUseCases) {
    this.reportUseCases = reportUseCases;
  }

  @GetMapping("/sales")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosReportSales
  public PagedResponse<PosSaleReportResponse> sales(
      @RequestParam long headquarterId,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(required = false) UUID shiftId,
      @RequestParam(required = false) Long productId,
      @ModelAttribute PageableRequest pageable) {
    return PagedResponse.map(
        reportUseCases.sales(filter(headquarterId, from, to, shiftId, productId, null), pageable.toPageable()),
        PosWebMapper::toSaleReportResponse);
  }

  @GetMapping("/products")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosReportProducts
  public PagedResponse<PosProductReportResponse> products(
      @RequestParam long headquarterId,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(required = false) UUID shiftId,
      @RequestParam(required = false) Long productId,
      @ModelAttribute PageableRequest pageable) {
    return PagedResponse.map(
        reportUseCases.products(
            filter(headquarterId, from, to, shiftId, productId, null), pageable.toPageable()),
        PosWebMapper::toProductReportResponse);
  }

  @GetMapping("/waste-cancellations")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosReportWasteCancellations
  public PagedResponse<PosLedgerEventReportResponse> wasteCancellations(
      @RequestParam long headquarterId,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(required = false) UUID shiftId,
      @RequestParam(required = false) String eventType,
      @ModelAttribute PageableRequest pageable) {
    return PagedResponse.map(
        reportUseCases.wasteCancellations(
            filter(headquarterId, from, to, shiftId, null, eventType), pageable.toPageable()),
        PosWebMapper::toLedgerEventReportResponse);
  }

  @GetMapping("/shift-closes")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosReportShiftCloses
  public PagedResponse<PosLedgerEventReportResponse> shiftCloses(
      @RequestParam long headquarterId,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(required = false) UUID shiftId,
      @ModelAttribute PageableRequest pageable) {
    return PagedResponse.map(
        reportUseCases.shiftCloses(
            filter(headquarterId, from, to, shiftId, null, null), pageable.toPageable()),
        PosWebMapper::toLedgerEventReportResponse);
  }

  private static PosReportFilterQuery filter(
      long headquarterId,
      Instant from,
      Instant to,
      UUID shiftId,
      Long productId,
      String eventType) {
    return new PosReportFilterQuery(headquarterId, from, to, shiftId, productId, eventType);
  }
}
