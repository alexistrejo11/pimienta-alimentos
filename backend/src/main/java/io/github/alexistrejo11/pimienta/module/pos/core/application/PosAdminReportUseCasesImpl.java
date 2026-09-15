package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.Headquarter;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosReportFilterQuery;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosSaleReportRow;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncEvent;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosAdminReportUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosDeviceRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSaleRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncEventRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncIncidentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosAdminReportUseCasesImpl implements PosAdminReportUseCases {

  private static final List<String> WASTE_CANCEL_TYPES =
      List.of("WASTE_RECORDED", "RESTOCK_RECORDED", "SALE_CANCELLED");

  private static final List<String> SHIFT_CLOSE_TYPES = List.of("SHIFT_CLOSED");

  private final PosSaleRepository saleRepository;
  private final PosSyncEventRepository eventRepository;
  private final PosSyncIncidentRepository incidentRepository;
  private final PosDeviceRepository deviceRepository;
  private final HeadquarterRepository headquarterRepository;

  public PosAdminReportUseCasesImpl(
      PosSaleRepository saleRepository,
      PosSyncEventRepository eventRepository,
      PosSyncIncidentRepository incidentRepository,
      PosDeviceRepository deviceRepository,
      HeadquarterRepository headquarterRepository) {
    this.saleRepository = saleRepository;
    this.eventRepository = eventRepository;
    this.incidentRepository = incidentRepository;
    this.deviceRepository = deviceRepository;
    this.headquarterRepository = headquarterRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PosSaleReportRow> sales(PosReportFilterQuery filter, Pageable pageable) {
    var salesPage = saleRepository.findAcceptedSales(filter, pageable);
    List<UUID> eventIds =
        salesPage.getContent().stream()
            .map(s -> s.getEventId())
            .distinct()
            .toList();
    Map<UUID, PosEventResultStatus> syncStatuses = eventRepository.findSyncStatusesByEventIds(eventIds);
    return salesPage.map(
        sale ->
            new PosSaleReportRow(
                sale,
                syncStatuses.getOrDefault(sale.getEventId(), PosEventResultStatus.ACCEPTED)));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PosProductReportRow> products(PosReportFilterQuery filter, Pageable pageable) {
    return saleRepository.findAcceptedProductTotals(filter, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PosSyncEvent> wasteCancellations(PosReportFilterQuery filter, Pageable pageable) {
    return eventRepository.findAcceptedByEventTypes(filter, WASTE_CANCEL_TYPES, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PosSyncEvent> shiftCloses(PosReportFilterQuery filter, Pageable pageable) {
    return eventRepository.findAcceptedByEventTypes(filter, SHIFT_CLOSE_TYPES, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PosReportSummaryRow> summary(List<Long> headquarterIds, Instant from, Instant to) {
    List<Long> hqIds = resolveHeadquarterIds(headquarterIds);
    List<PosReportSummaryRow> rows = new ArrayList<>(hqIds.size());
    for (Long hqId : hqIds) {
      PosSalesSummary sales = saleRepository.summarizeAcceptedSales(hqId, from, to);
      PosOpenProductSummary openProducts = saleRepository.summarizeOpenProducts(hqId, from, to);
      long wasteCount =
          eventRepository.countAcceptedByEventType(hqId, from, to, "WASTE_RECORDED");
      long cancellationCount =
          eventRepository.countAcceptedByEventType(hqId, from, to, "SALE_CANCELLED");
      Instant lastShiftClosedAt =
          eventRepository.findLastAcceptedEventAt(hqId, from, to, "SHIFT_CLOSED");
      rows.add(
          new PosReportSummaryRow(
              hqId,
              sales.salesCentavos(),
              sales.ticketCount(),
              wasteCount,
              cancellationCount,
              lastShiftClosedAt,
              incidentRepository.countOpenByHeadquarterId(hqId),
               deviceRepository.countByHeadquarterId(hqId),
               openProducts.openProductCentavos(),
               openProducts.openProductLineCount(),
               openProducts.openProductTicketCount(),
               openProducts.openProductPendingReviewCount()));
    }
    return List.copyOf(rows);
  }

  private List<Long> resolveHeadquarterIds(List<Long> headquarterIds) {
    if (headquarterIds != null) {
      return List.copyOf(headquarterIds);
    }
    return headquarterRepository.findAll(PageRequest.of(0, 500)).stream()
        .filter(h -> h.getDeletedAt() == null)
        .map(Headquarter::getId)
        .toList();
  }
}
