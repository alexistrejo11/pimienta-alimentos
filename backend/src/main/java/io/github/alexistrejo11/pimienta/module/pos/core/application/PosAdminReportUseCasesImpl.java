package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosReportFilterQuery;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSale;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncEvent;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosAdminReportUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSaleRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncEventRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosAdminReportUseCasesImpl implements PosAdminReportUseCases {

  private static final List<String> WASTE_CANCEL_TYPES =
      List.of("WASTE_RECORDED", "RESTOCK_RECORDED", "SALE_CANCELLED");

  private static final List<String> SHIFT_CLOSE_TYPES = List.of("SHIFT_CLOSED");

  private final PosSaleRepository saleRepository;
  private final PosSyncEventRepository eventRepository;

  public PosAdminReportUseCasesImpl(
      PosSaleRepository saleRepository, PosSyncEventRepository eventRepository) {
    this.saleRepository = saleRepository;
    this.eventRepository = eventRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PosSale> sales(PosReportFilterQuery filter, Pageable pageable) {
    return saleRepository.findAcceptedSales(filter, pageable);
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
}
